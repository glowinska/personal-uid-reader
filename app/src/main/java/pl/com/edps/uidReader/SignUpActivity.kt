package pl.com.edps.uidReader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.wifi.hotspot2.pps.Credential
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class SignUpActivity : AppCompatActivity() {

//    private var edUsername = EditText(this)
//    private var edPassword = EditText(this)
//    private var edConfirmPassword = EditText(this)
//    private var btnCreateUser = Button(this)


    @SuppressLint("CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        var edUsername = findViewById<EditText>(R.id.ed_username)
        var edPassword = findViewById<EditText>(R.id.ed_password)
        var edConfirmPassword = findViewById<EditText>(R.id.ed_confirm_password)
        var btnCreateUser = findViewById<Button>(R.id.btn_create_user)

        btnCreateUser.setOnClickListener {

            var strPassword = edPassword.text.toString()
            var strConfirmPassword = edConfirmPassword.text.toString()
            var strUsername = edUsername.text.toString()

            if (strPassword == strConfirmPassword) {
                var sharedPreferences = getPreferences(Context.MODE_PRIVATE)
                var editor = sharedPreferences.edit()
                editor.putString("Password", strPassword)
                editor.putString("Username", strUsername)
                editor.apply()

                this.finish()
            }
//            val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
//            intent.putExtra("key", "Kotlin")
//            startActivity(intent)
        }
    }
}