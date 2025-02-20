import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

fun main() {
    // --- ЗАДАЧА 1: Потокобезопасный счётчик ---
    var counter = 0
    val lock = Any()
    val counterThreads = List(5) {
        thread {
            repeat(1000) {
                synchronized(lock) { counter++ }
            }
        }
    }
    counterThreads.forEach { it.join() }
    println("Task 1: Final counter value: $counter")

    // --- ЗАДАЧА 2: Потокобезопасный список ---
    val numbers = CopyOnWriteArrayList<Int>()
    val numberThreads = List(10) {
        thread { for (i in 1..100) numbers.add(i) }
    }
    numberThreads.forEach { it.join() }
    println("Task 2: Total numbers: ${numbers.size}")

    // --- ЗАДАЧА 3: Пул потоков ---
    val executor3 = Executors.newFixedThreadPool(4)
    for (i in 1..20) executor3.execute { println("Task 3: Task $i by ${Thread.currentThread().name}") }
    executor3.shutdown()

    // --- ЗАДАЧА 4: Банк и переводы ---
    class BankAccount(var balance: Int) {
        private val lock = ReentrantLock()
        fun transfer(to: BankAccount, amount: Int) {
            lock.lock()
            try {
                if (balance >= amount) {
                    balance -= amount
                    to.balance += amount
                    println("Task 4: Transferred $amount from ${Thread.currentThread().name}")
                }
            } finally {
                lock.unlock()
            }
        }
    }
    val account1 = BankAccount(1000)
    val account2 = BankAccount(1000)
    val bankThreads = List(10) { thread { account1.transfer(account2, 50) } }
    bankThreads.forEach { it.join() }
    println("Task 4: Account1 balance: ${account1.balance}, Account2 balance: ${account2.balance}")

    // --- ЗАДАЧА 5: CyclicBarrier ---
    val barrier = CyclicBarrier(5) { println("Task 5: All threads reached the barrier") }
    repeat(5) {
        thread {
            println("Task 5: ${Thread.currentThread().name} working...")
            Thread.sleep((500..1500).random().toLong())
            barrier.await()
        }
    }

    // --- ЗАДАЧА 6: Semaphore ---
    val semaphore = Semaphore(2)
    repeat(5) {
        thread {
            semaphore.acquire()
            println("Task 6: ${Thread.currentThread().name} accessing resource")
            Thread.sleep(1000)
            semaphore.release()
        }
    }

    // --- ЗАДАЧА 7: Callable и Future ---
    val executor7 = Executors.newFixedThreadPool(10)
    val futures = (1..10).map { executor7.submit(Callable { factorial(it) }) }
    futures.forEachIndexed { index, future -> println("Task 7: Factorial of ${index + 1} is ${future.get()}") }
    executor7.shutdown()

    // --- ЗАДАЧА 8: Производственная линия ---
    val queue = ArrayBlockingQueue<Int>(10)
    thread { for (i in 1..10) { queue.put(i); println("Task 8: Produced $i"); Thread.sleep(500) } }
    thread { repeat(10) { println("Task 8: Consumed ${queue.take()}") } }

    // --- ЗАДАЧА 9: Параллельная сортировка ---
    val array = (1..100).shuffled().toIntArray()
    val parts = array.toList().chunked(array.size / 4)
    val executor9 = Executors.newFixedThreadPool(4)
    val sortedParts = parts.map { part -> executor9.submit<List<Int>> { part.sorted() } }.map { it.get() }
    executor9.shutdown()
    println("Task 9: Sorted array: ${sortedParts.flatten()}")

    // --- ЗАДАЧА 10: Обед философов ---
    class Fork
    class Philosopher(private val leftFork: Fork, private val rightFork: Fork) {
        fun dine() {
            synchronized(leftFork) { synchronized(rightFork) {
                println("Task 10: ${Thread.currentThread().name} is eating")
                Thread.sleep(1000)
            }}
        }
    }
    val forks = List(5) { Fork() }
    val philosophers = List(5) { Philosopher(forks[it], forks[(it + 1) % 5]) }
    val philosopherThreads = philosophers.map { thread { it.dine() } }
    philosopherThreads.forEach { it.join() }

    // --- ЗАДАЧА 11: Параллельное умножение матриц ---
    val matrixA = arrayOf(intArrayOf(1, 2), intArrayOf(3, 4))
    val matrixB = arrayOf(intArrayOf(2, 0), intArrayOf(1, 2))
    val result = Array(matrixA.size) { IntArray(matrixB[0].size) }
    val executor11 = Executors.newFixedThreadPool(2)
    matrixA.indices.forEach { row -> executor11.execute { multiplyRow(matrixA, matrixB, row, result) } }
    executor11.shutdown()
    println("Task 11: Matrix result:\n${result.joinToString("\n") { it.joinToString(" ") }}")

    // --- ЗАДАЧА 12: Таймер ---
    val timerThread = thread {
        repeat(10) { println("Task 12: Time: $it sec"); Thread.sleep(1000) }
    }
    Thread.sleep(10000)
    println("Task 12: Timer stopped")
}

// Функция факториала для Задачи 7
fun factorial(n: Int): Long = if (n == 0) 1 else n * factorial(n - 1)

// Функция умножения строки матрицы для Задачи 11
fun multiplyRow(matrixA: Array<IntArray>, matrixB: Array<IntArray>, row: Int, result: Array<IntArray>) {
    for (j in matrixB[0].indices) result[row][j] = (matrixA[row].indices).sumOf { matrixA[row][it] * matrixB[it][j] }
}
