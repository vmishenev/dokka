import kotlin.native.ref.WeakReference

actual typealias WeakReference<T> = WeakReference<T>

fun main() {
    println("Hello, Kotlin/Native!")
}