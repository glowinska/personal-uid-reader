package pl.com.edps.uidReader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LoginActivity : AppCompatActivity() {

//    private var edUsername = EditText(this)
//    private var edPassword = EditText(this)
//    private var btnLogin = Button(this)
//    private var btnSignUp = Button(this)

    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var edUsername = findViewById<EditText>(R.id.ed_username)
        var edPassword = findViewById<EditText>(R.id.ed_password)
        var btnLogin = findViewById<Button>(R.id.btn_login)
        var btnSignUp = findViewById<Button>(R.id.btn_signup)

        btnSignUp.setOnClickListener {
            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
            intent.putExtra("key", "Kotlin")
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            var sharedPreferences = getPreferences(Context.MODE_PRIVATE)
            var strUsername = sharedPreferences.getString("Username", null)
            var strPassword = sharedPreferences.getString("Password", null)

            var edPassword = edPassword.text.toString()
            var edUsername = edUsername.text.toString()

            if (edPassword == strPassword) {
                if (edUsername == strUsername) {
                    Toast.makeText(this, "Login succeed!", Toast.LENGTH_SHORT)
                    Log.d("LOGIN","Login succeed!")
                } else {
                    Toast.makeText(this, "Login failed!", Toast.LENGTH_SHORT)
                    Log.d("LOGIN","Login failed!")
                }
            } else {
                Toast.makeText(this, "Login failed!", Toast.LENGTH_SHORT)
                Log.d("LOGIN","Login failed!")
            }

//            val intent = Intent(this@LoginActivity, SignUpActivity::class.java)
//            intent.putExtra("key", "Kotlin")
//            startActivity(intent)
        }

    }
}