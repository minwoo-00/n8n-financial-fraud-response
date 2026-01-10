# 📌 n8n 기반 이상금융거래 대응 자동화 플랫폼

> Rule-based Scoring & Automated Financial Fraud Response using n8n
> 

---

## 💡 개요

본 프로젝트는 **금융 서비스에서 발생하는 로그인 및 거래 로그를 기반으로
이상금융거래(FDS)를 탐지하고 대응을 자동화**하는 시스템이다.

Java(Spring Boot) 기반의 간단한 금융 웹 애플리케이션에서 발생한 이벤트를

**n8n 워크플로우로 수집**하고,

**룰 기반 점수화 방식**으로 이상 여부를 판단한 뒤

알림, 차단, 기록 등 **대응 프로세스를 자동화**한다.

> 본 프로젝트의 핵심 목적은
> 
> 
> **“이상 탐지 로직이 아닌, 보안 대응 자동화 오케스트레이션”** 이다.
> 

---

개발 팀원

---

## 🎯 프로젝트 목표

- 로그 기반 이벤트를 **자동으로 수집**
- 룰/점수 기반으로 **이상 거래를 판단**
- 판단 결과에 따라 **대응을 자동으로 실행**
- 이 전 과정을 **n8n 워크플로우로 오케스트레이션**

---

## 🗃️ 시스템 아키텍처

<img width="2702" height="1563" alt="image" src="https://github.com/user-attachments/assets/dc7cccfc-2d93-4256-aa99-266d1ea9868e" />


### 구성 요소 역할

| 구성 요소 | 역할 |
| --- | --- |
| Java(Spring Boot) | 로그인/송금 기능, 이벤트 생성 |
| n8n | 이벤트 수집, 점수화, 판단, 대응 자동화 |
| Google Sheets | 룰셋(조건/점수) 관리 |
| DB / ES (선택) | 이상 이벤트 저장 및 조회 |
| Slack | 관리자 알림 |

---

## 🛠️ 기술 스택 (Tech Stack)

### Backend

- Java 17
- Spring Boot
- REST API

### Automation

- n8n
- Webhook / Code / IF / HTTP Request 노드

### Storage

- Google Sheets (Rule Management)
- PostgreSQL / Elasticsearch (Anomaly Logs, Optional)

### Notification

- Slack Webhook

---

## 4. 주요 기능

### ✅ 1) 금융 기능 시뮬레이션

- 로그인
- 잔액 조회
- 송금

### ✅ 2) 로그 기반 이벤트 수집

- 로그인 성공/실패
- 송금 요청
- IP, User-Agent, 금액, 시간 정보 포함

### ✅ 3) 룰 기반 점수화 (Rule-based Scoring)

- 룰은 **Google Sheets**에서 관리
- n8n이 룰을 조회하여 점수 계산

예시 룰:

| Rule | 설명 | 점수 |
| --- | --- | --- |
| NEW_IP | 신규 IP 로그인 | +20 |
| HIGH_AMOUNT | 평균 대비 고액 송금 | +30 |
| RAPID_TRANSFER | 로그인 직후 송금 | +30 |

---

### ✅ 4) 이상금융거래 대응 자동화

| 위험도 | 대응 |
| --- | --- |
| LOW | 로그 저장 |
| MEDIUM | Slack 알림 |
| HIGH | Slack 알림 + 거래 차단(API 호출) |

---

### ✅ 5) 이상 로그 관리

- 이상 이벤트만 별도로 저장
- 관리자가 빠르게 확인 가능

---

## 5. n8n 활용 포인트 (Why n8n?)

본 프로젝트에서 **n8n은 단순 알림 도구가 아니라 중앙 판단 엔진** 역할을 수행한다.

- Webhook 기반 이벤트 수집
- 룰 데이터 외부화 (Google Sheets)
- Code Node를 통한 점수화
- 위험도별 분기 처리 (IF / Switch)
- Slack / API / DB 연동 자동화
- Human-in-the-loop 확장 가능

> “탐지는 애플리케이션에서,
> 
> 
> 판단과 대응은 n8n에서 수행”
> 

---

## 7. 예시 이벤트 로그 (Sample Event)

```json
{
"eventType":"TRANSFER",
"userId":"user123",
"amount":5000000,
"loginIp":"10.0.0.5",
"requestIp":"203.0.113.10",
"userAgent":"Chrome/120",
"timestamp":"2026-01-08T17:30:00"
}

```

---

## 트러블 슈팅 / 프로젝트 과정

---

## 9. 결론

> “n8n을 중심으로 금융 로그를 분석하고
이상금융거래 대응을 자동화한 보안 오케스트레이션 시스템”
>
