# Wild Duel

Wild Duel은 두 팀으로 나뉘어 정해진 시간 동안 자원을 파밍하고, 마지막까지 살아남는 팀이 승리하는 서바이벌 PvP 마인크래프트 플러그인입니다.

## ✨ 주요 기능

- **팀 기반 PvP**: 레드 팀과 블루 팀으로 나뉘어 경쟁합니다.
- **게임 단계**: 로비 → 카운트다운 → 파밍 → 전투 → 게임 종료 → (처음) 의 흐름으로 진행됩니다.
- **설정 가능한 타이머**: 파밍 시간을 자유롭게 조절할 수 있습니다.
- **자동으로 줄어드는 월드 보더**: 전투 단계가 시작되면 월드 보더가 점차 줄어들어 플레이어들을 중앙으로 모읍니다.
- **팀원 TPA 시스템**: 같은 팀원에게만 TPA 요청을 보낼 수 있으며, 관리자가 쿨타임 및 요청 만료 시간을 설정할 수 있습니다.
- **관리자 및 플레이어 GUI**: 직관적인 GUI를 통해 게임 설정, 팀 선택 등을 쉽게 할 수 있습니다.
- **커스텀 시작 아이템**: 게임 시작 시 모든 플레이어에게 지급될 아이템을 GUI를 통해 설정할 수 있습니다.
- **커스텀 메시지**: `messages.yml` 파일을 통해 플러그인의 거의 모든 메시지를 원하는 대로 수정할 수 있습니다.

## 🎮 명령어

### 플레이어 명령어

| 명령어 | 설명 |
| --- | --- |
| `/tpa <플레이어>` | 같은 팀원에게 텔레포트 요청을 보냅니다. |
| `/tpacancel` | 보낸 TPA 요청을 취소합니다. |
| `/tparesponse <accept/deny> <플레이어>` | 받은 TPA 요청을 수락하거나 거절합니다. |
| `/팀선택` | 팀 선택 GUI를 엽니다. |

### 관리자 명령어 (`/wd` 또는 `/wildduel`)

| 명령어 | 설명 | 권한 |
| --- | --- | --- |
| `/wd start` | 게임을 시작합니다. | `wildduel.admin` |
| `/wd reset` | 게임을 초기화하고 로비로 되돌립니다. | `wildduel.admin` |
| `/wd setprep <시간(초)>` | 파밍 준비 시간을 설정합니다. | `wildduel.admin` |
| `/wd autosmelt <on/off>` | 자동 제련 기능을 켜거나 끕니다. | `wildduel.admin` |
| `/wd teamselection <on/off>` | 플레이어가 직접 팀을 선택할 수 있게 할지 설정합니다. | `wildduel.admin` |
| `/wd opengui` | 관리자 설정 GUI를 엽니다. | `wildduel.admin` |
| `/wd team <assign/reset>` | 모든 플레이어의 팀을 랜덤으로 배정하거나 초기화합니다. | `wildduel.admin` |
| `/wd teamadmin` | 팀 관리 GUI를 엽니다. | `wildduel.admin` |
| `/wd tpa <refreshall/refresh [플레이어]>` | 모든/특정 플레이어의 TPA 쿨타임을 초기화합니다. | `wildduel.admin` |
| `/wd startitem` | 기본 시작 아이템 설정 GUI를 엽니다. | `wildduel.admin` |

## 🔧 설정

### `config.yml`

- `tpa-request-timeout`: TPA 요청이 만료되기까지의 시간 (초)
- `tpa-cooldown-seconds`: TPA 명령어의 재사용 대기시간 (초)
- `default-start-items`: 기본 시작 아이템 목록 (인게임에서 GUI를 통해 쉽게 편집 가능)

### `messages.yml`

플러그인에서 사용되는 대부분의 메시지를 이곳에서 수정할 수 있습니다. `{player}`, `{team}`과 같은 플레이스홀더를 사용하여 동적인 메시지를 만들 수 있습니다.

## 🛠️ 설치

1.  [wildduel-3.3.jar](https://github.com/boulmyong/Wild_Duel/releases/download/v3.3/wildduel-3.3.jar) 파일을 다운로드 합니다.
2.  해당 파일을 Spigot/Paper 서버의 `plugins/` 폴더에 넣습니다.
3.  서버를 재시작합니다.


질문이나 요청 사항이 있다면 디스코드 **qnfaud**로 디엠 보내주세요.
