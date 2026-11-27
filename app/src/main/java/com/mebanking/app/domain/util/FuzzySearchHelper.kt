package com.mebanking.app.domain.util

import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.min

/**
 * Accent-insensitive and typo-tolerant fuzzy search utility used across
 * Transaction History and Contacts screens.
 */
object FuzzySearchHelper {
    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val wordSplitRegex = Regex("[\\s,.;:/\\-_#|()]+")

    fun normalize(input: String): String {
        val replaced = input.replace('đ', 'd').replace('Đ', 'D')
        val nfd = Normalizer.normalize(replaced, Normalizer.Form.NFD)
        return diacriticsRegex.replace(nfd, "").lowercase().trim()
    }

    fun matches(query: String, fields: Iterable<String?>): Boolean {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isBlank()) return true

        val normalizedFields = fields
            .mapNotNull { it?.takeIf { s -> s.isNotBlank() }?.let(::normalize) }
        if (normalizedFields.isEmpty()) return false

        val corpus = normalizedFields.joinToString(" ")
        val compactCorpus = corpus.filter { it.isLetterOrDigit() }
        val corpusWords = corpus.split(wordSplitRegex).filter { it.isNotBlank() }

        val queryTokens = normalizedQuery.split(Regex("\\s+")).filter { it.isNotBlank() }
        return queryTokens.all { token ->
            tokenMatchesCorpus(token, corpus, compactCorpus, corpusWords)
        }
    }

    private fun tokenMatchesCorpus(
        token: String,
        corpus: String,
        compactCorpus: String,
        corpusWords: List<String>,
    ): Boolean {
        if (corpus.contains(token)) return true

        val compactToken = token.filter { it.isLetterOrDigit() }
        if (compactToken.length >= 2 && compactCorpus.contains(compactToken)) return true

        // Subsequence match on individual words (e.g. "chuyenk" -> "chuyenkhoan", "vnp" -> "vnpay")
        if (compactToken.length >= 3 && corpusWords.any { word -> isSubsequence(compactToken, word) }) {
            return true
        }

        // Typo tolerance via bounded Damerau-Levenshtein (optimal string alignment) distance
        val maxDistance = when {
            compactToken.length >= 7 -> 2
            compactToken.length >= 4 -> 1
            else -> 0
        }
        if (maxDistance > 0 && corpusWords.any { word ->
                word.length >= 3 &&
                    abs(word.length - compactToken.length) <= maxDistance &&
                    damerauLevenshtein(compactToken, word) <= maxDistance
            }
        ) {
            return true
        }

        return false
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        if (needle.length > haystack.length) return false
        var ni = 0
        for (ch in haystack) {
            if (ch == needle[ni]) {
                ni++
                if (ni == needle.length) return true
            }
        }
        return false
    }

    private fun damerauLevenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var best = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost,
                )
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                    best = min(best, dp[i - 2][j - 2] + 1)
                }
                dp[i][j] = best
            }
        }
        return dp[m][n]
    }
}
