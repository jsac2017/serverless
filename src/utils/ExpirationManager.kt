package utils

class ExpirationManager<T>(private val expirationTimeFun: (T) -> Int?) {
    private val expired = mutableListOf<T>()
    private val waitingForExpiration = hashMapOf<Int, MutableList<T>>()
    private var currentTime = 0

    fun tick() {
        val expiringNow = waitingForExpiration.remove(currentTime) ?: listOf<T>()
        expiringNow.forEach {
            val expirationTime = expirationTimeFun(it)!!
            if (expirationTime == currentTime) {
                expired.add(it)
            } else {
                watchForExpiration(it)
            }
        }

        currentTime++
    }

    fun watchForExpiration(element: T) {
        val expirationTime = expirationTimeFun(element) ?: return

        if (expirationTime < currentTime) {
            expired.add(element)
            return
        }

        if (!waitingForExpiration.containsKey(expirationTime)) {
            waitingForExpiration[expirationTime] = mutableListOf()
        }
        waitingForExpiration[expirationTime]?.add(element)
    }

    fun pollExpired(): Collection<T> {
        val result = expired.toList()
        expired.clear()
        return result
    }
}