package com.guiyec.similar

import com.google.gson.Gson
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReferenceArray

object Similar {
    var defaultGson: Gson = Gson()

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2> combine(task1: Task<O1>, 
                         task2: Task<O2>): Task<Tuple2<O1, O2>> {
        val mainTask = Task<Tuple2<O1, O2>>()
        val results = AtomicReferenceArray<Any?>(2)
        val successCount = AtomicInteger(2)

        fun createTuple(): Tuple2<O1, O2> {
            return Tuple2(
                results.get(0) as O1,
                results.get(1) as O2)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2)
        mainTask.registerCancel(task1, task2)
        task1.registerSuccess(0)
        task2.registerSuccess(1)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3> combine(task1: Task<O1>,
                             task2: Task<O2>,
                             task3: Task<O3>): Task<Tuple3<O1, O2, O3>> {
        val mainTask = Task<Tuple3<O1, O2, O3>>()

        val results = AtomicReferenceArray<Any?>(3)
        val successCount = AtomicInteger(3)

        fun createTuple(): Tuple3<O1, O2, O3> {
            return Tuple3(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3)
        mainTask.registerCancel(task1, task2, task3)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3, O4> combine(task1: Task<O1>,
                                 task2: Task<O2>,
                                 task3: Task<O3>,
                                 task4: Task<O4>): Task<Tuple4<O1, O2, O3, O4>> {
        val mainTask = Task<Tuple4<O1, O2, O3, O4>>()

        val results = AtomicReferenceArray<Any?>(4)
        val successCount = AtomicInteger(4)

        fun createTuple(): Tuple4<O1, O2, O3, O4> {
            return Tuple4(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3,
                results.get(3) as O4)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3, task4)
        mainTask.registerCancel(task1, task2, task3, task4)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)
        task4.registerSuccess(3)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3, O4, O5> combine(task1: Task<O1>,
                                     task2: Task<O2>,
                                     task3: Task<O3>,
                                     task4: Task<O4>,
                                     task5: Task<O5>): Task<Tuple5<O1, O2, O3, O4, O5>> {
        val mainTask = Task<Tuple5<O1, O2, O3, O4, O5>>()

        val results = AtomicReferenceArray<Any?>(5)
        val successCount = AtomicInteger(5)

        fun createTuple(): Tuple5<O1, O2, O3, O4, O5> {
            return Tuple5(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3,
                results.get(3) as O4,
                results.get(4) as O5)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3, task4, task5)
        mainTask.registerCancel(task1, task2, task3, task4, task5)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)
        task4.registerSuccess(3)
        task5.registerSuccess(4)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3, O4, O5, O6> combine(task1: Task<O1>,
                                         task2: Task<O2>,
                                         task3: Task<O3>,
                                         task4: Task<O4>,
                                         task5: Task<O5>,
                                         task6: Task<O6>): Task<Tuple6<O1, O2, O3, O4, O5, O6>> {
        val mainTask = Task<Tuple6<O1, O2, O3, O4, O5, O6>>()

        val results = AtomicReferenceArray<Any?>(6)
        val successCount = AtomicInteger(6)

        fun createTuple(): Tuple6<O1, O2, O3, O4, O5, O6> {
            return Tuple6(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3,
                results.get(3) as O4,
                results.get(4) as O5,
                results.get(5) as O6)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3, task4, task5, task6)
        mainTask.registerCancel(task1, task2, task3, task4, task5, task6)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)
        task4.registerSuccess(3)
        task5.registerSuccess(4)
        task6.registerSuccess(5)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3, O4, O5, O6, O7> combine(task1: Task<O1>,
                                             task2: Task<O2>,
                                             task3: Task<O3>,
                                             task4: Task<O4>,
                                             task5: Task<O5>,
                                             task6: Task<O6>,
                                             task7: Task<O7>): Task<Tuple7<O1, O2, O3, O4, O5, O6, O7>> {
        val mainTask = Task<Tuple7<O1, O2, O3, O4, O5, O6, O7>>()

        val results = AtomicReferenceArray<Any?>(7)
        val successCount = AtomicInteger(7)

        fun createTuple(): Tuple7<O1, O2, O3, O4, O5, O6, O7> {
            return Tuple7(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3,
                results.get(3) as O4,
                results.get(4) as O5,
                results.get(5) as O6,
                results.get(6) as O7)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3, task4, task5, task6, task7)
        mainTask.registerCancel(task1, task2, task3, task4, task5, task6, task7)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)
        task4.registerSuccess(3)
        task5.registerSuccess(4)
        task6.registerSuccess(5)
        task7.registerSuccess(6)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3, O4, O5, O6, O7, O8> combine(task1: Task<O1>,
                                                 task2: Task<O2>,
                                                 task3: Task<O3>,
                                                 task4: Task<O4>,
                                                 task5: Task<O5>,
                                                 task6: Task<O6>,
                                                 task7: Task<O7>,
                                                 task8: Task<O8>): Task<Tuple8<O1, O2, O3, O4, O5, O6, O7, O8>> {
        val mainTask = Task<Tuple8<O1, O2, O3, O4, O5, O6, O7, O8>>()

        val results = AtomicReferenceArray<Any?>(8)
        val successCount = AtomicInteger(8)

        fun createTuple(): Tuple8<O1, O2, O3, O4, O5, O6, O7, O8> {
            return Tuple8(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3,
                results.get(3) as O4,
                results.get(4) as O5,
                results.get(5) as O6,
                results.get(6) as O7,
                results.get(7) as O8)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3, task4, task5, task6, task7, task8)
        mainTask.registerCancel(task1, task2, task3, task4, task5, task6, task7, task8)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)
        task4.registerSuccess(3)
        task5.registerSuccess(4)
        task6.registerSuccess(5)
        task7.registerSuccess(6)
        task8.registerSuccess(7)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3, O4, O5, O6, O7, O8, O9> combine(task1: Task<O1>,
                                                     task2: Task<O2>,
                                                     task3: Task<O3>,
                                                     task4: Task<O4>,
                                                     task5: Task<O5>,
                                                     task6: Task<O6>,
                                                     task7: Task<O7>,
                                                     task8: Task<O8>,
                                                     task9: Task<O9>): Task<Tuple9<O1, O2, O3, O4, O5, O6, O7, O8, O9>> {
        val mainTask = Task<Tuple9<O1, O2, O3, O4, O5, O6, O7, O8, O9>>()

        val results = AtomicReferenceArray<Any?>(9)
        val successCount = AtomicInteger(9)

        fun createTuple(): Tuple9<O1, O2, O3, O4, O5, O6, O7, O8, O9> {
            return Tuple9(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3,
                results.get(3) as O4,
                results.get(4) as O5,
                results.get(5) as O6,
                results.get(6) as O7,
                results.get(7) as O8,
                results.get(8) as O9)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3, task4, task5, task6, task7, task8, task9)
        mainTask.registerCancel(task1, task2, task3, task4, task5, task6, task7, task8, task9)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)
        task4.registerSuccess(3)
        task5.registerSuccess(4)
        task6.registerSuccess(5)
        task7.registerSuccess(6)
        task8.registerSuccess(7)
        task9.registerSuccess(8)

        return mainTask
    }

    @Suppress("UNCHECKED_CAST")
    fun <O1, O2, O3, O4, O5, O6, O7, O8, O9, O10> combine(task1: Task<O1>,
                                                          task2: Task<O2>,
                                                          task3: Task<O3>,
                                                          task4: Task<O4>,
                                                          task5: Task<O5>,
                                                          task6: Task<O6>,
                                                          task7: Task<O7>,
                                                          task8: Task<O8>,
                                                          task9: Task<O9>,
                                                          task10: Task<O10>): Task<Tuple10<O1, O2, O3, O4, O5, O6, O7, O8, O9, O10>> {
        val mainTask = Task<Tuple10<O1, O2, O3, O4, O5, O6, O7, O8, O9, O10>>()

        val results = AtomicReferenceArray<Any?>(10)
        val successCount = AtomicInteger(10)

        fun createTuple(): Tuple10<O1, O2, O3, O4, O5, O6, O7, O8, O9, O10> {
            return Tuple10(
                results.get(0) as O1,
                results.get(1) as O2,
                results.get(2) as O3,
                results.get(3) as O4,
                results.get(4) as O5,
                results.get(5) as O6,
                results.get(6) as O7,
                results.get(7) as O8,
                results.get(8) as O9,
                results.get(9) as O10)
        }

        fun Task<*>.registerSuccess(index: Int) {
            sink { data ->
                results.set(index, data)
                if (successCount.decrementAndGet() == 0) {
                    mainTask.complete(createTuple())
                }
            }
        }

        mainTask.registerFail(task1, task2, task3, task4, task5, task6, task7, task8, task9, task10)
        mainTask.registerCancel(task1, task2, task3, task4, task5, task6, task7, task8, task9, task10)
        task1.registerSuccess(0)
        task2.registerSuccess(1)
        task3.registerSuccess(2)
        task4.registerSuccess(3)
        task5.registerSuccess(4)
        task6.registerSuccess(5)
        task7.registerSuccess(6)
        task8.registerSuccess(7)
        task9.registerSuccess(8)
        task10.registerSuccess(9)

        return mainTask
    }
}

private fun Task<*>.registerFail(vararg promises: Task<*>) {
    val failCount = AtomicInteger(0)
    promises.forEach { promise ->
        promise.catch { e ->
            if (failCount.incrementAndGet() == 1) {
                this.fail(e)
            }
        }
    }
}

private fun Task<*>.registerCancel(vararg promises: Task<*>) {
    cancelBlock = {
        promises.forEach { it.cancel() }
    }
}

data class Tuple2
<O1, O2>
    (val first: O1,
     val second: O2)

data class Tuple3
<O1, O2, O3>
    (val first: O1,
     val second: O2,
     val third: O3)

data class Tuple4
<O1, O2, O3, O4>
    (val first: O1,
     val second: O2,
     val third: O3,
     val fourth: O4)

data class Tuple5
<O1, O2, O3, O4, O5>
    (val first: O1,
     val second: O2,
     val third: O3,
     val fourth: O4,
     val fifth: O5)

data class Tuple6
<O1, O2, O3, O4, O5, O6>
    (val first: O1,
     val second: O2,
     val third: O3,
     val fourth: O4,
     val fifth: O5,
     val sixth: O6)

data class Tuple7
<O1, O2, O3, O4, O5, O6, O7>
    (val first: O1,
     val second: O2,
     val third: O3,
     val fourth: O4,
     val fifth: O5,
     val sixth: O6,
     val seventh: O7)

data class Tuple8
<O1, O2, O3, O4, O5, O6, O7, O8>
    (val first: O1,
     val second: O2,
     val third: O3,
     val fourth: O4,
     val fifth: O5,
     val sixth: O6,
     val seventh: O7,
     val eighth: O8)

data class Tuple9
<O1, O2, O3, O4, O5, O6, O7, O8, O9>
    (val first: O1,
     val second: O2,
     val third: O3,
     val fourth: O4,
     val fifth: O5,
     val sixth: O6,
     val seventh: O7,
     val eighth: O8,
     val ninth: O9)

data class Tuple10
<O1, O2, O3, O4, O5, O6, O7, O8, O9, O10>
    (val first: O1,
     val second: O2,
     val third: O3,
     val fourth: O4,
     val fifth: O5,
     val sixth: O6,
     val seventh: O7,
     val eighth: O8,
     val ninth: O9,
     val tenth: O10)