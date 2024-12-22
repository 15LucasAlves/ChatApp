object UserRepository {
    private val users = mutableMapOf<String, String>()

    fun registerUser(email: String, password: String): Boolean {
        return if (!users.containsKey(email)) {
            users[email] = password
            true
        } else {
            false
        }
    }

    fun loginUser(email: String, password: String): Boolean {
        return users[email] == password
    }
} 