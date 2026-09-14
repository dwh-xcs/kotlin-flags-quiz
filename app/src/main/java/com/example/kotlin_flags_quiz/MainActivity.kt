package com.example.kotlin_flags_quiz

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var editNome: EditText
    private lateinit var btnIniciar: Button
    private lateinit var btnSair: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editNome = findViewById(R.id.editNome)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnSair = findViewById(R.id.btnSair)

        // Botão iniciar começa desabilitado por padrão
        btnIniciar.isEnabled = false

        // Monitora a digitação do nome
        editNome.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val nomePreenchido = !s.isNullOrBlank()
                btnIniciar.isEnabled = nomePreenchido
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        btnIniciar.setOnClickListener {
            val nomeUsuario = editNome.text.toString()
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("USER_NAME", nomeUsuario)
            }
            startActivity(intent)
        }

        btnSair.setOnClickListener {
            finishAffinity() // Encerra o aplicativo
        }
    }
}