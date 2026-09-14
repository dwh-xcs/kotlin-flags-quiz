package com.example.kotlin_flags_quiz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class PerguntaApi(
    val imageUrl: String,
    val respostaCorreta: String,
    val alternativas: List<String>
)

class QuizActivity : AppCompatActivity() {

    private lateinit var txtTituloPergunta: TextView
    private lateinit var imgBandeira: ImageView
    private lateinit var radioGroup: RadioGroup
    private lateinit var rbOpcao1: RadioButton
    private lateinit var rbOpcao2: RadioButton
    private lateinit var rbOpcao3: RadioButton
    private lateinit var rbOpcao4: RadioButton
    private lateinit var btnResponder: Button

    private var indiceAtual = 0
    private var acertos = 0
    private lateinit var nomeUsuario: String
    private var listaPerguntas = mutableListOf<PerguntaApi>()

    // Pool robusto de bandeiras locais (garante funcionamento offline/imediato sem tela preta)
    private val paisesFallback = listOf(
        Pair("Brasil", "https://flagcdn.com/w640/br.png"),
        Pair("Argentina", "https://flagcdn.com/w640/ar.png"),
        Pair("Canadá", "https://flagcdn.com/w640/ca.png"),
        Pair("Japão", "https://flagcdn.com/w640/jp.png"),
        Pair("França", "https://flagcdn.com/w640/fr.png"),
        Pair("Alemanha", "https://flagcdn.com/w640/de.png"),
        Pair("Itália", "https://flagcdn.com/w640/it.png"),
        Pair("Espanha", "https://flagcdn.com/w640/es.png"),
        Pair("Austrália", "https://flagcdn.com/w640/au.png"),
        Pair("Portugal", "https://flagcdn.com/w640/pt.png"),
        Pair("Estados Unidos", "https://flagcdn.com/w640/us.png"),
        Pair("México", "https://flagcdn.com/w640/mx.png")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        nomeUsuario = intent.getStringExtra("USER_NAME") ?: "Convidado"

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@QuizActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            }
        })

        txtTituloPergunta = findViewById(R.id.txtTituloPergunta)
        imgBandeira = findViewById(R.id.imgBandeira)
        radioGroup = findViewById(R.id.radioGroup)
        rbOpcao1 = findViewById(R.id.rbOpcao1)
        rbOpcao2 = findViewById(R.id.rbOpcao2)
        rbOpcao3 = findViewById(R.id.rbOpcao3)
        rbOpcao4 = findViewById(R.id.rbOpcao4)
        btnResponder = findViewById(R.id.btnResponder)

        btnResponder.isEnabled = false

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            btnResponder.isEnabled = checkedId != -1
        }

        // Inicializa imediatamente com o fallback local para desenhar a tela sem atraso
        gerarPerguntas(paisesFallback)
        carregarPergunta()

        // Em segundo plano, tenta buscar os dados atualizados da API v5
        sincronizarComApi()

        btnResponder.setOnClickListener {
            val idSelecionado = radioGroup.checkedRadioButtonId
            if (idSelecionado != -1 && listaPerguntas.isNotEmpty() && indiceAtual < listaPerguntas.size) {
                val radioButtonSelecionado = findViewById<RadioButton>(idSelecionado)
                val respostaEscolhida = radioButtonSelecionado.text.toString()

                if (respostaEscolhida == listaPerguntas[indiceAtual].respostaCorreta) {
                    acertos++
                }

                indiceAtual++

                if (indiceAtual < listaPerguntas.size) {
                    carregarPergunta()
                } else {
                    val intent = Intent(this, RankingActivity::class.java).apply {
                        putExtra("USER_NAME", nomeUsuario)
                        putExtra("ACERTOS", acertos)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun gerarPerguntas(fontePaises: List<Pair<String, String>>) {
        val listaMutavel = fontePaises.toMutableList()
        listaMutavel.shuffle()
        val selecionados = listaMutavel.take(10)
        val perguntasGeradas = mutableListOf<PerguntaApi>()
        val todosNomes = listaMutavel.map { it.first }

        for (pais in selecionados) {
            val nomeCorreto = pais.first
            val urlBandeira = pais.second
            val erradas = todosNomes.filter { it != nomeCorreto }.shuffled().take(3)
            val alternativas = (erradas + nomeCorreto).shuffled()

            perguntasGeradas.add(PerguntaApi(urlBandeira, nomeCorreto, alternativas))
        }
        listaPerguntas = perguntasGeradas
    }

    private fun sincronizarComApi() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Usando a API estável RestCountries v3.1
                val url = URL("https://restcountries.com/v3.1/all?fields=name,flags")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonArray = JSONArray(response)

                    val paisesApi = mutableListOf<Pair<String, String>>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val nameObj = obj.optJSONObject("name")
                        val nomePais = nameObj?.optString("common") ?: ""
                        val flagsObj = obj.optJSONObject("flags")
                        val imageUrl = flagsObj?.optString("png") ?: ""

                        if (nomePais.isNotBlank() && imageUrl.isNotBlank()) {
                            paisesApi.add(Pair(nomePais, imageUrl))
                        }
                    }

                    if (paisesApi.size >= 10) {
                        gerarPerguntas(paisesApi)
                        withContext(Dispatchers.Main) {
                            // Atualiza a primeira pergunta caso o usuário ainda esteja nela
                            if (indiceAtual == 0 && listaPerguntas.isNotEmpty()) {
                                carregarPergunta()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun carregarPergunta() {
        if (listaPerguntas.isEmpty() || indiceAtual >= listaPerguntas.size) return
        val pergunta = listaPerguntas[indiceAtual]

        txtTituloPergunta.text = "Qual é o país desta bandeira?"

        Glide.with(this)
            .load(pergunta.imageUrl)
            .into(imgBandeira)

        rbOpcao1.text = pergunta.alternativas[0]
        rbOpcao2.text = pergunta.alternativas[1]
        rbOpcao3.text = pergunta.alternativas[2]
        rbOpcao4.text = pergunta.alternativas[3]

        radioGroup.clearCheck()
        btnResponder.isEnabled = false
    }
}