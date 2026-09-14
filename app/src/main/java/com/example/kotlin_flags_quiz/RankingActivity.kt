package com.example.kotlin_flags_quiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

data class PontuacaoItem(val nome: String, val acertos: Int)

class RankingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ranking)

        val nomeUsuario = intent.getStringExtra("USER_NAME") ?: "Anônimo"
        val acertosAtuais = intent.getIntExtra("ACERTOS", 0)

        val containerRanking = findViewById<LinearLayout>(R.id.containerRanking)
        val btnResponderNovamente = findViewById<Button>(R.id.btnResponderNovamente)
        val btnTelaPrincipal = findViewById<Button>(R.id.btnTelaPrincipal)

        // SharedPreferences para salvar o histórico em formato JSON
        val prefs = getSharedPreferences("QuizRankingPrefs", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("lista_ranking", "[]")

        val listaRanking = mutableListOf<PontuacaoItem>()
        val jsonArray = JSONArray(jsonString)

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            listaRanking.add(PontuacaoItem(obj.getString("nome"), obj.getInt("acertos")))
        }

        // Adiciona a tentativa atual na lista
        listaRanking.add(PontuacaoItem(nomeUsuario, acertosAtuais))

        // Ordena da maior pontuação para a menor e pega apenas os 5 melhores
        val top5 = listaRanking.sortedByDescending { it.acertos }.take(5)

        // Salva a lista atualizada de volta
        val novoJsonArray = JSONArray()
        for (item in top5) {
            val obj = JSONObject().apply {
                put("nome", item.nome)
                put("acertos", item.acertos)
            }
            novoJsonArray.put(obj)
        }

        val editor = prefs.edit()
        editor.putString("lista_ranking", novoJsonArray.toString())
        editor.apply()

        // Exibe dinamicamente no layout com as medalhas
        containerRanking.removeAllViews()
        top5.forEachIndexed { index, item ->
            val textView = TextView(this).apply {
                textSize = 18f
                setPadding(0, 12, 0, 12)

                val medalha = when (index) {
                    0 -> "🥇 1º - "
                    1 -> "🥈 2º - "
                    2 -> "🥉 3º - "
                    else -> "    ${index + 1}º - "
                }
                text = "$medalha ${item.nome}: ${item.acertos} acertos"
            }
            containerRanking.addView(textView)
        }

        btnResponderNovamente.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java).apply {
                putExtra("USER_NAME", nomeUsuario)
            }
            startActivity(intent)
            finish()
        }

        btnTelaPrincipal.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}