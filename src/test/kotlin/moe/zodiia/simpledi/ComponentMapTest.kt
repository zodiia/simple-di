package moe.zodiia.simpledi

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.util.UUID
import kotlin.reflect.typeOf

class ComponentMapTest {
    interface IFoo {
        var i: Int
    }

    class Foo : IFoo {
        override var i = 0
        val rand = UUID.randomUUID().toString()
    }

    class Bar(injectionScope: InjectionScope, componentMap: ComponentMap) {
        val foo by injection<Foo>(injectionScope, componentMap)
    }

    class BarWithConstructor(val foo: Foo)
    class BarWithOptionalConstructor(val foo: IFoo = Foo())
    class BarWithRequiredConstructor(val foo: IFoo)

    @Test
    fun `Test request scope`() {
        val componentMap = ComponentMap()
        val bar = Bar(InjectionScope.REQUEST, componentMap)

        Assertions.assertNotEquals(bar.foo.rand, bar.foo.rand)
        bar.foo.i += 1
        Assertions.assertEquals(0, bar.foo.i)
    }

    @Test
    fun `Test instance scope`() {
        val componentMap = ComponentMap()
        val bar1 = Bar(InjectionScope.INSTANCE, componentMap)
        val bar2 = Bar(InjectionScope.INSTANCE, componentMap)

        Assertions.assertEquals(bar1.foo.rand, bar1.foo.rand)
        Assertions.assertNotEquals(bar1.foo.rand, bar2.foo.rand)
        bar1.foo.i = 1
        bar2.foo.i = 2
        Assertions.assertNotEquals(bar1.foo.i, bar2.foo.i)
    }

    @Test
    fun `Test thread scope`() {
        val componentMap = ComponentMap()
        val bar1 = Bar(InjectionScope.THREAD, componentMap)
        val bar2 = Bar(InjectionScope.THREAD, componentMap)
        var rand: String = bar1.foo.rand

        val th1 = Thread {
            bar1.foo.i = 1
            bar2.foo.i = 2
            rand = bar1.foo.rand
            Assertions.assertEquals(bar1.foo.i, bar2.foo.i)
        }
        th1.start()
        th1.join()

        Assertions.assertEquals(0, bar1.foo.i)
        Assertions.assertEquals(0, bar2.foo.i)
        Assertions.assertNotEquals(bar1.foo.rand, rand)
    }

    @Test
    fun `Test runtime scope`() {
        val componentMap = ComponentMap()
        val bar1 = Bar(InjectionScope.RUNTIME, componentMap)
        val bar2 = Bar(InjectionScope.RUNTIME, componentMap)

        Assertions.assertEquals(bar1.foo.rand, bar2.foo.rand)
        bar1.foo.i = 1
        Assertions.assertEquals(1, bar2.foo.i)
        bar2.foo.i = 2
        Assertions.assertEquals(2, bar1.foo.i)
    }

    @Test
    fun `Test runtime scope with different component maps`() {
        val componentMap1 = ComponentMap()
        val componentMap2 = ComponentMap()
        val bar1 = Bar(InjectionScope.RUNTIME, componentMap1)
        val bar2 = Bar(InjectionScope.RUNTIME, componentMap2)
        val bar3 = Bar(InjectionScope.RUNTIME, componentMap2)

        Assertions.assertNotEquals(bar1.foo.rand, bar2.foo.rand)
        Assertions.assertEquals(bar2.foo.rand, bar3.foo.rand)
    }

    @Test
    fun `Test manually adding the instance`() {
        val componentMap = ComponentMap()
        val foo = Foo()
        componentMap.addInstance(foo, typeOf<Foo>(), InjectionScope.RUNTIME)
        val bar1 = Bar(InjectionScope.RUNTIME, componentMap)

        Assertions.assertEquals(foo.rand, bar1.foo.rand)

        val bar2 = Bar(InjectionScope.THREAD, componentMap)

        Assertions.assertNotEquals(foo.rand, bar2.foo.rand)

        val thread = Thread {
            componentMap.addInstance(foo, typeOf<Foo>(), InjectionScope.THREAD, Thread.currentThread().id)

            val bar3 = Bar(InjectionScope.THREAD, componentMap)

            Assertions.assertEquals(foo.rand, bar3.foo.rand)
        }
        thread.start()
        thread.join()
    }

    @Test
    fun `Test has instance of`() {
        val componentMap = ComponentMap()
        val pid = Thread.currentThread().id

        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.RUNTIME))
        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid))
        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid + 1))

        Bar(InjectionScope.RUNTIME, componentMap).foo.rand

        Assertions.assertTrue(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.RUNTIME))
        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid))
        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid + 1))

        Bar(InjectionScope.THREAD, componentMap).foo.rand

        Assertions.assertTrue(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.RUNTIME))
        Assertions.assertTrue(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid))
        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid + 1))

        componentMap.addInstance(Foo(), typeOf<Foo>(), InjectionScope.THREAD, pid + 1)

        Assertions.assertTrue(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.RUNTIME))
        Assertions.assertTrue(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid))
        Assertions.assertTrue(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.THREAD, pid + 1))

        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.REQUEST))
        Assertions.assertFalse(componentMap.hasInstanceFor(typeOf<Foo>(), InjectionScope.INSTANCE))
    }

    @Test
    fun `Test injecting invalid types`() {
        Assertions.assertThrows(IllegalStateException::class.java) {
            val foo by injection<IFoo>()

            foo.i = 1
        }
    }

    @Test
    fun `Test injecting type with constructor arguments`() {
        val componentMap = ComponentMap()
        val bar by injection<BarWithConstructor>(InjectionScope.RUNTIME, componentMap)

        Assertions.assertDoesNotThrow {
            bar.foo.rand
        }
    }

    @Test
    fun `Test injecting with optional arguments`() {
        val componentMap = ComponentMap()
        val bar by injection<BarWithOptionalConstructor>(InjectionScope.RUNTIME, componentMap)

        Assertions.assertDoesNotThrow {
            Assertions.assertEquals(0, bar.foo.i)
        }
    }

    @Test
    fun `Test injecting with required arguments`() {
        val componentMap = ComponentMap()
        val bar by injection<BarWithRequiredConstructor>(InjectionScope.RUNTIME, componentMap)

        Assertions.assertThrows(IllegalStateException::class.java) {
            bar.foo.i
        }
    }
}
