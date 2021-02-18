package pl.com.edps.uidReader

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sign_up.*


class SignUpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        btn_create_user.setOnClickListener {
            if (ed_password.text.toString() == ed_confirm_password.text.toString()) {
                val sharedPref = getSharedPreferences(
                    "com.example.myapp.PREFERENCE_FILE_KEY", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putString("Password", ed_password.text.toString())
                editor.putString("Username", ed_username.text.toString())
                editor.apply()
                this.finish()
            }
        }
    }
}