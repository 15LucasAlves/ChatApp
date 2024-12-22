import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

object FirebaseRepository {
    private val database = Firebase.database.reference
    private val usersRef = database.child("users")

    suspend fun registerUser(email: String, password: String): Boolean {
        return try {
            val sanitizedEmail = email.replace(".", "_").replace("@", "_at_")
            val userSnapshot = usersRef.child(sanitizedEmail).get().await()
            
            if (!userSnapshot.exists()) {
                val user = User(email, password)
                usersRef.child(sanitizedEmail).setValue(user).await()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun loginUser(email: String, password: String): Boolean {
        return try {
            val sanitizedEmail = email.replace(".", "_").replace("@", "_at_")
            val userSnapshot = usersRef.child(sanitizedEmail).get().await()
            
            if (userSnapshot.exists()) {
                val user = userSnapshot.getValue(User::class.java)
                user?.password == password
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
} 