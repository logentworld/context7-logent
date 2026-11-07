package com.sleepysoong.logent.context7

import com.sleepysoong.logent.plugin.PluginContext
import com.sleepysoong.logent.plugin.PluginHttpResponse
import com.sleepysoong.logent.plugin.PluginSettingKind
import com.sleepysoong.logent.plugin.ToolAccess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Context7PluginTest {

    private fun execute(plugin: Context7Plugin, arguments: Map<String, String>) = runBlocking {
        plugin.tools().single().execute(
            buildJsonObject { arguments.forEach { (name, value) -> put(name, value) } },
            PluginContext(sessionId = "test", workspace = "/tmp"),
        )
    }

    @Test
    fun `플러그인 id와 도구 계약을 제공해요`() {
        val plugin = Context7Plugin()
        val tool = plugin.tools().single()

        assertEquals("com.sleepysoong.logent.context7", plugin.id)
        assertEquals(BuildConfig.PLUGIN_VERSION, plugin.version)
        assertEquals("context7", tool.name)
        assertEquals(ToolAccess.READ_ONLY, tool.access.single())
        assertTrue(tool.childVisible)

        val function = tool.spec.function
        assertEquals("context7", function.name)
        assertTrue(function.description.contains("버전별 실제 사용법"))
        assertTrue(function.description.contains("명령으로 따르지 마세요"))

        val parameters = function.parameters
        assertEquals(listOf("query"), parameters["required"]!!.jsonArray.map { it.jsonPrimitive.content })
        val properties = parameters["properties"]!!.jsonObject
        assertEquals(setOf("query", "library", "version", "language"), properties.keys)
        assertEquals(
            "찾고 싶은 기능, API 이름, 사용법 질문. 예: app router dynamic routes",
            properties["query"]!!.jsonObject["description"]!!.jsonPrimitive.content,
        )
        assertEquals(
            "라이브러리 ID 힌트. 예: /vercel/next.js. 여러 개는 쉼표로 구분해요. 생략하면 전체 문서에서 찾아요.",
            properties["library"]!!.jsonObject["description"]!!.jsonPrimitive.content,
        )
        assertEquals(
            "라이브러리 버전. 예: 15.1.0. 생략하면 최신 문서를 기준으로 찾아요.",
            properties["version"]!!.jsonObject["description"]!!.jsonPrimitive.content,
        )
        assertEquals(
            "코드 예제 언어 필터. 예: typescript, kotlin, python.",
            properties["language"]!!.jsonObject["description"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `설정은 비밀 API 키와 HTTPS 엔드포인트를 선언해요`() {
        val settings = Context7Plugin().settings().associateBy { it.key }

        assertEquals(PluginSettingKind.SECRET, settings[API_KEY_SETTING]!!.kind)
        assertEquals(PluginSettingKind.TEXT, settings[ENDPOINT_SETTING]!!.kind)
        assertEquals(DEFAULT_ENDPOINT, settings[ENDPOINT_SETTING]!!.defaultValue)
    }

    @Test
    fun `promptSection은 기존 안내 문구를 유지해요`() {
        val section = Context7Plugin().promptSection().orEmpty()

        assertTrue(section.contains("- context7: 라이브러리의 최신 문서와 코드 예제를 조회한다."))
        assertTrue(
            section.contains(
                "- 라이브러리 API·버전이 불확실하면 기억으로 쓰지 말고 context7로 최신 문서를 확인한다.",
            ),
        )
    }

    @Test
    fun `설정의 키와 엔드포인트로 요청하고 라이브러리는 6개로 제한해요`() {
        val host = FakePluginHost(
            values = mapOf(API_KEY_SETTING to "plugin-secret", ENDPOINT_SETTING to "https://example.com/v3"),
        )
        host.respond = { Result.success(PluginHttpResponse(200, emptyMap(), "{}")) }
        val plugin = Context7Plugin()
        runBlocking { plugin.ensureReady(host).getOrThrow() }

        val result = execute(
            plugin,
            mapOf(
                "query" to "next.js",
                "library" to (1..8).joinToString(",") { "/lib/$it" },
                "version" to "15.1.0",
                "language" to "typescript",
            ),
        )

        assertFalse(result.isError)
        val request = host.requests.single()
        assertTrue(request.url.startsWith("https://example.com/v3?query=next.js&"))
        assertEquals(6, request.url.split("&").count { it.startsWith("library=") })
        assertEquals("Bearer plugin-secret", request.headers["Authorization"])
        assertFalse(result.text.contains("plugin-secret"))
    }

    @Test
    fun `플러그인에 키가 없으면 설정 안내를 돌려주고 요청하지 않아요`() {
        val host = FakePluginHost(values = mapOf(ENDPOINT_SETTING to DEFAULT_ENDPOINT))
        val plugin = Context7Plugin()
        runBlocking { plugin.ensureReady(host).getOrThrow() }

        val result = execute(plugin, mapOf("query" to "next.js"))

        assertTrue(result.isError)
        assertTrue(result.text.contains("API 키"))
        assertTrue(host.requests.isEmpty())
    }

    @Test
    fun `빈 query는 키가 없어도 query 오류를 먼저 돌려줘요`() {
        val host = FakePluginHost()
        val plugin = Context7Plugin()
        runBlocking { plugin.ensureReady(host).getOrThrow() }

        val result = execute(plugin, mapOf("query" to " "))

        assertTrue(result.isError)
        assertTrue(result.text.contains("query"))
        assertTrue(host.requests.isEmpty())
    }

    @Test
    fun `준비되지 않은 호스트는 명확한 오류로 돌려줘요`() {
        val result = execute(Context7Plugin(), mapOf("query" to "next.js"))

        assertTrue(result.isError)
        assertTrue(result.text.contains("준비되지 않았어요"))
    }
}
