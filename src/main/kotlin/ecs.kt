import kotlin.reflect.KClass

interface Component

interface RegisteredComponents {
    val components: Map<KClass<out Component>, () -> Component>
}

interface System<in Ctx> {
    var enabled: Boolean
    fun processEntity(entity: Int)
    fun init(w: World<Ctx>)
    fun before() {}
    fun after() {}
}

abstract class AbstractSystem<Ctx> : System<Ctx> {
    override var enabled: Boolean = true
    lateinit var world: World<Ctx>

    override fun init(w: World<Ctx>) {
        world = w
    }

    open fun shouldRun(entity: Int): Boolean {
        if (!enabled) return false
        return true
    }

    abstract fun doProcessEntity(entity: Int)

    override fun processEntity(entity: Int) {
        if (shouldRun(entity)) {
            doProcessEntity(entity)
        }
    }
}

abstract class ComponentSystem<Ctx>(private val requiredComponents: Set<KClass<out Component>>) :
    AbstractSystem<Ctx>() {
    override fun shouldRun(entity: Int): Boolean {
        return super.shouldRun(entity) && requiredComponents.all { world.component(entity, it) != null }
    }
}

abstract class Component1System<T : Component, Ctx>(private val cclass: KClass<T>) :
    ComponentSystem<Ctx>(setOf(cclass)) {
    abstract fun doProcessEntity(component: T)

    override fun doProcessEntity(entity: Int) {
        doProcessEntity(world.component(entity, cclass)!!)
    }
}

abstract class Component2System<T1 : Component, T2 : Component, Ctx>(
    private val cclass1: KClass<T1>,
    private val cclass2: KClass<T2>
) : ComponentSystem<Ctx>(setOf(cclass1, cclass2)) {
    abstract fun doProcessEntity(component1: T1, component2: T2)

    override fun doProcessEntity(entity: Int) {
        doProcessEntity(
            world.component(entity, cclass1)!!,
            world.component(entity, cclass2)!!
        )
    }
}

abstract class Component3System<T1 : Component, T2 : Component, T3 : Component, Ctx>(
    private val cclass1: KClass<T1>,
    private val cclass2: KClass<T2>,
    private val cclass3: KClass<T3>
) : ComponentSystem<Ctx>(setOf(cclass1, cclass2, cclass3)) {
    abstract fun doProcessEntity(component1: T1, component2: T2, component3: T3)

    override fun doProcessEntity(entity: Int) {
        doProcessEntity(
            world.component(entity, cclass1)!!,
            world.component(entity, cclass2)!!,
            world.component(entity, cclass3)!!
        )
    }
}

abstract class Component4System<T1 : Component, T2 : Component, T3 : Component, T4 : Component, Ctx>(
    private val cclass1: KClass<T1>,
    private val cclass2: KClass<T2>,
    private val cclass3: KClass<T3>,
    private val cclass4: KClass<T4>
) : ComponentSystem<Ctx>(setOf(cclass1, cclass2, cclass3, cclass4)) {
    abstract fun doProcessEntity(component1: T1, component2: T2, component3: T3, component4: T4)

    override fun doProcessEntity(entity: Int) {
        doProcessEntity(
            world.component(entity, cclass1)!!,
            world.component(entity, cclass2)!!,
            world.component(entity, cclass3)!!,
            world.component(entity, cclass4)!!
        )
    }
}

typealias EntityByComponentType = MutableMap<KClass<out Component>, MutableMap<Int, Component?>>


class World<out Ctx>(
    val registeredComponents: RegisteredComponents,
    val globals: Ctx
) {
    private var components: EntityByComponentType = mutableMapOf()
    private var systems = mutableListOf<System<Ctx>>()
    private var entities: MutableList<Int> = mutableListOf()
    private var freeEntities = mutableListOf<Int>()
    private var pendingCreations = mutableMapOf<Int, CreateEntity>()
    private var delayed = mutableListOf<() -> Unit>()
    private var maxId = 0
    private val tagsMap = mutableMapOf<String, Int>()

    val tags: Map<String, Int>
        get() = tagsMap

    val entityCount: Int
        get() = entities.size

    fun registerSystem(system: System<Ctx>) {
        system.init(this)
        systems.add(system)
    }

    fun tick() {
        systems.forEach { it.before() }
        entities.forEach { i ->
            systems.forEach { system ->
                if (system.enabled) {
                    system.processEntity(i)
                }
            }
        }
        systems.forEach { it.after() }
        delayed.forEach { it() }
        delayed.clear()
        pendingCreations.values.forEach(this::processPendingCreations)
        pendingCreations.clear()
    }

    fun createEntity(): Int {
        val id = if (freeEntities.size > 0) freeEntities.removeAt(0) else maxId++
        pendingCreations[id] = CreateEntity(id, mutableMapOf())
        return id
    }

    fun deleteEntity(id: Int) {
        pendingCreations.remove(id)
        delay {
            entities.remove(id)
            components.forEach { cm ->
                cm.value[id] = null
            }
        }
    }

    fun <T : Component> addComponent(e: Int, c: KClass<T>): T {
        if (!components.containsKey(c)) {
            components[c] = mutableMapOf()
        }
        val component = registeredComponents.components[c]!!().unsafeCast<T>()
        if (pendingCreations.containsKey(e)) {
            pendingCreations[e]!!.cs[c] = component
        } else {
            components[c]!![e] = component
        }
        return component
    }

    fun <T : Component> deleteComponent(e: Int, c: KClass<T>) {
        if (!components.containsKey(c)) {
            throw IllegalStateException("Component type is wrong $c")
        }
        if (pendingCreations.containsKey(e)) {
            pendingCreations[e]!!.cs.remove(c)
        } else {
            components[c]!!.remove(e)
        }
    }

    fun <T : Component> component(e: Int, c: KClass<T>): T? = components[c]?.get(e)?.unsafeCast<T>()

    /**
     * Use this to update components at the end of the frame.
     * Do all the computations before calling this, otherwise
     * there is no real reason to delay the update since it will depend
     * on potentially updated values of the other components anyway.
     *
     * Be careful not to capture dependencies (rvalues) that can change by the time
     * this is actually evaluated.
     */
    fun delay(op: () -> Unit) {
        delayed.add(op)
    }

    fun tag(tag: String, id: Int) {
        delay {
            tagsMap[tag] = id
        }
    }

    fun untag(tag: String) {
        delay {
            tagsMap -= tag
        }
    }

    private fun processPendingCreations(op: CreateEntity) {
        entities.add(op.id)
        op.cs.forEach { c ->
            components[c.key]!![op.id] = c.value
        }
    }

    data class CreateEntity(val id: Int, var cs: MutableMap<KClass<out Component>, Component>)
}

open class EmptyContext()