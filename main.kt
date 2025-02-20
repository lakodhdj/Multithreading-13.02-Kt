import java.util.concurrent.*
import kotlin.random.Random

// --- КОНФИГУРАЦИЯ ---
object Config {
    const val ISLAND_WIDTH = 10
    const val ISLAND_HEIGHT = 5
    const val CYCLE_DURATION_MS = 1000L
    val INITIAL_ANIMALS = mapOf("Wolf" to 5, "Rabbit" to 10)
}

// --- КЛАССЫ ДЛЯ ООП ---
// Базовый класс Животного
abstract class Animal(val name: String, var x: Int, var y: Int) {
    var hunger = 100

    abstract fun move(island: Island)
    abstract fun eat(island: Island)
    abstract fun reproduce(island: Island)
}

// Хищники
abstract class Predator(name: String, x: Int, y: Int) : Animal(name, x, y) {
    override fun eat(island: Island) {
        val cell = island.grid[x][y]
        val prey = cell.animals.filterIsInstance<Herbivore>().randomOrNull()
        if (prey != null && Random.nextInt(100) < 60) {
            cell.animals.remove(prey)
            hunger = 100
            println("$name съел ${prey.name}!")
        } else hunger -= 10
    }
}

// Травоядные
abstract class Herbivore(name: String, x: Int, y: Int) : Animal(name, x, y) {
    override fun eat(island: Island) {
        val cell = island.grid[x][y]
        if (cell.plants > 0) {
            cell.plants--
            hunger = 100
            println("$name поел траву!")
        } else hunger -= 10
    }
}

// Конкретные животные
class Wolf(x: Int, y: Int) : Predator("Wolf", x, y) {
    override fun move(island: Island) { randomMove(island) }
    override fun reproduce(island: Island) { reproduceIfPossible<Wolf>(island) }
}

class Rabbit(x: Int, y: Int) : Herbivore("Rabbit", x, y) {
    override fun move(island: Island) { randomMove(island) }
    override fun reproduce(island: Island) { reproduceIfPossible<Rabbit>(island) }
}

// --- КЛАСС ОСТРОВА ---
class Island(val width: Int, val height: Int) {
    val grid = Array(width) { x -> Array(height) { y -> Location(x, y) } }

    fun populate() {
        Config.INITIAL_ANIMALS.forEach { (name, count) ->
            repeat(count) {
                val x = Random.nextInt(width)
                val y = Random.nextInt(height)
                val animal = when (name) {
                    "Wolf" -> Wolf(x, y)
                    "Rabbit" -> Rabbit(x, y)
                    else -> null
                }
                animal?.let { grid[x][y].animals.add(it) }
            }
        }
    }

    fun printStats() {
        val animalCount = grid.flatten().sumOf { it.animals.size }
        val plantCount = grid.flatten().sumOf { it.plants }
        println("Животных: $animalCount, Растений: $plantCount")
    }
}

// Клетка острова
class Location(val x: Int, val y: Int) {
    val animals = mutableListOf<Animal>()
    var plants = Random.nextInt(5)
}

// --- ПОМОЩНИКИ ---
fun Animal.randomMove(island: Island) {
    val dx = Random.nextInt(-1, 2)
    val dy = Random.nextInt(-1, 2)
    val newX = (x + dx).coerceIn(0, island.width - 1)
    val newY = (y + dy).coerceIn(0, island.height - 1)
    island.grid[x][y].animals.remove(this)
    island.grid[newX][newY].animals.add(this)
    x = newX
    y = newY
}

inline fun <reified T : Animal> Animal.reproduceIfPossible(island: Island) {
    val cell = island.grid[x][y]
    if (cell.animals.count { it is T } > 1 && Random.nextBoolean()) {
        cell.animals.add(T::class.constructors.first().call(x, y))
        println("$name размножился!")
    }
}

// --- ГЛАВНАЯ ФУНКЦИЯ ---
fun main() {
    val island = Island(Config.ISLAND_WIDTH, Config.ISLAND_HEIGHT)
    island.populate()

    val scheduler = Executors.newScheduledThreadPool(3)

    // Задача 1: Рост растений
    scheduler.scheduleAtFixedRate({
        island.grid.flatten().forEach { it.plants++ }
        println("Растения выросли!")
    }, 0, Config.CYCLE_DURATION_MS, TimeUnit.MILLISECONDS)

    // Задача 2: Жизненный цикл животных
    scheduler.scheduleAtFixedRate({
        val executor = Executors.newFixedThreadPool(10)
        island.grid.flatten().forEach { location ->
            location.animals.forEach { animal ->
                executor.submit {
                    animal.eat(island)
                    animal.move(island)
                    animal.reproduce(island)
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(500, TimeUnit.MILLISECONDS)
    }, 0, Config.CYCLE_DURATION_MS, TimeUnit.MILLISECONDS)

    // Задача 3: Вывод статистики
    scheduler.scheduleAtFixedRate({
        island.printStats()
    }, 0, Config.CYCLE_DURATION_MS, TimeUnit.MILLISECONDS)

    // Остановка по условию
    Thread.sleep(10000)
    scheduler.shutdown()
    println("Симуляция завершена!")
}
