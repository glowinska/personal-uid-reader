package pl.com.edps.uidReader

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        btn_signup.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            intent.putExtra("key", "Kotlin")
            startActivity(intent)
        }

        btn_login.setOnClickListener {
            val sharedPreferences = getSharedPreferences(
                "com.example.myapp.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
            val strUsername = sharedPreferences.getString("Username", null)
            val strPassword = sharedPreferences.getString("Password", null)

            if (strUsername != null && strUsername.equals(
                    ed_username.text.toString(),
                    ignoreCase = true
                )
            ) {
                if (strPassword != null && strPassword.equals(
                        ed_password.text.toString(),
                        ignoreCase = true
                    )
                ) {
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("key", "Kotlin")
                    startActivity(intent)
                } else {
                    Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT)
                        .show()
                }
            } else {
                Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
