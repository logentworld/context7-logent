# context7-logent

[upstash/context7](https://github.com/upstash/context7)의 실제 GitHub 포크예요. 원본 이력과
MIT 고지를 그대로 보존하고, Logent의 `.lpg` 플러그인에 필요한 코드만 `logent` 브랜치에
얹었어요.

- 포크: `logentworld/context7-logent` (parent `upstash/context7`)
- upstream 커밋: `1ccf58e6e7f68357ef95c8b84181255d5ffa5c05`
- upstream 패키지 버전: `1.0.0` (`package.json`)
- 원본 README 원문: [`README.upstream.md`](README.upstream.md)
- 변경 내역: [`NOTICE`](NOTICE)

`logent-plugin.json`을 `scripts/plugin-pack.py build --definition logent-plugin.json`의
공통 정의로 사용해요. 플러그인 key는 `sleepysoong:context7`, `dependencies`는 `[]`예요.
`sleepysoong`는 명시한 publisher 별칭이며 GitHub 인증 계정이나 소유권을 뜻하지 않아요.
key와 의존성에서 생성한 manifest의 `requires`는 서명 metadata에 포함되고,
`content-public-key.der.b64`에 고정한 공개키로 서명과 파일 해시를 검증해요.

## 무엇을 하나요

`com.sleepysoong.logent.context7` 플러그인이 `context7` 도구를 제공해요. 라이브러리의 공식 문서에서
최신 코드 예제와 설명을 찾아 모델에 돌려줘요. 네트워크 호출은 플러그인이 직접 하지 않고
Logent 호스트의 공용 HTTPS 기능(`PluginHost.http`)만 써요.

- 도구 이름: `context7`
- 권한: 읽기 전용, 자식 에이전트에 노출
- 입력: `query`(필수), `library`, `version`, `language`
- 출력: 최대 24,000자. 넘으면 `도구 출력 한도에서 잘렸어요.`를 붙여 잘라요.
- 응답 본문: 최대 1MiB. 호스트에 1MiB 한도를 요청하고 한 번 더 바이트 단위로 확인해요.
- 문서 발췌는 항상 `<untrusted_web>` 경계 안에 넣고, 본문이 경계 태그를 흉내 내면
  `&lt;untrusted_web&gt;`로 무력화해요.

## 설정

| 키 | 종류 | 설명 |
| --- | --- | --- |
| `api-key` | SECRET | Context7 API 키예요. 없으면 도구가 설정 안내 오류를 돌려줘요. |
| `endpoint` | TEXT | v3 검색 주소예요. 기본값은 `https://context7.com/api/v3/search`이고 HTTPS만 허용해요. |

키는 요청의 `Authorization: Bearer` 헤더에만 넣어요. 로그·진행 문구·오류문구·도구 출력에는
키를 남기지 않고, 엔드포인트 검사도 요청 전에 해요.

## 저장소 구조

```text
plugin/                 Logent Kotlin 플러그인 (GPL-3.0)
plugin/src/main/...     com.sleepysoong.logent.context7 플러그인과 v3 검색 클라이언트
plugin/src/test/...     파싱·파라미터·키 누락·응답 한도 단위 테스트
scripts/                Gradle 실행·테스트·서명 .lpg 패키징
gradle/, gradlew*       Gradle 9.7.1 wrapper
plugin-api.lock         Logent Plugin API 4.0.0 고정
content-public-key.der.b64  .lpg 서명 검증용 공개키
```

upstream의 `setup/`, Claude 마켓플레이스 메타데이터, TypeScript 패키지와 문서는 저장소에
원본 그대로 남아 있지만 `.lpg`에는 넣지 않아요. 플러그인 패키지에는 Kotlin 플러그인 코드만
들어가요.

## 검증

Plugin API 4.0.0은 GitHub Packages에서 받아요. 읽기 권한 토큰을 환경 변수로 넘겨 주세요.

```bash
export GITHUB_ACTOR=<GitHub 사용자>
export GITHUB_TOKEN=<packages:read 권한 토큰>
./scripts/test.sh
```

JVM 단위 테스트와 Android lint를 실행해요. 테스트는 실제 Context7 서버를 부르지 않고
가짜 transport로 요청·응답을 검사해요.

서명된 `.lpg`까지 만들려면 Git 밖에 둔 개인 키 파일을 지정해요.

```bash
LOGENT_PLUGIN_SIGNING_KEY_FILE=/secure/logent-plugin.pem ./scripts/build.sh
```

`build/dist/`에 `.lpg`, `.aar`, 소스 ZIP, `SHA256SUMS`가 생겨요. 공개키를 따로 쓰려면
`LOGENT_PLUGIN_PUBLIC_KEY_FILE`을 지정해요.

## upstream 병합

`logent` 브랜치는 upstream 이력을 그대로 두고 Logent 추가분만 얹어요. upstream 변경을
가져올 때는 원본 README를 `README.upstream.md`로 다시 보존하고, `UPSTREAM_COMMIT`과
`NOTICE`의 커밋을 갱신해요.

## 라이선스

- upstream Context7 코드와 문서: MIT (`LICENSE` 원문 보존)
- Logent가 작성한 Kotlin 플러그인과 Gradle·스크립트: GPL-3.0 (`plugin/LICENSE`)
