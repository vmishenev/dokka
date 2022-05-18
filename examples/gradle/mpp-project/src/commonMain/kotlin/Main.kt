public expect class WeakReference<T: Any>(referred: T) {
    public fun get(): T?
}

fun main() {
    println("Hello, Kotlin/Native!")
}