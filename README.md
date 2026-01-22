# 📌 n8n 기반 이상금융거래 대응 자동화 플랫폼

> Rule-based Scoring & Automated Financial Fraud Response using n8n
> 

---

## 💡 개요

n8n을 활용하여 **금융 서비스에서 발생하는 로그인 및 거래 로그를 기반으로
이상금융거래를 탐지하고 대응하는 과정을 자동화** 하는 프로젝트이다.

<img width="856" height="362" alt="image" src="https://github.com/user-attachments/assets/ad35f858-f5d3-4949-bd99-8f607fc88889" />


---
## 👨‍👩‍👦 개발 팀원
<table>
  <tr>
    <td align="center">
      <a href="https://github.com/minwoo-00"><img src="https://github.com/minwoo-00.png" width="100px;" alt=""/></a>
      <div style="width:100px;border-top:1px solid #d0d7de;margin:8px 0;"></div>
      <b>조민우</b>
      <div style="width:60px;border-top:1px solid #d0d7de;margin:6px 0;"></div>
      <a href="https://github.com/minwoo-00">@minwoo-00</a>
    </td>
    <td align="center">
      <a href="https://github.com/ssh221"><img src="https://github.com/ssh221.png" width="100px;" alt=""/></a>
      <div style="width:100px;border-top:1px solid #d0d7de;margin:8px 0;"></div>
      <b>신성혁</b>
      <div style="width:60px;border-top:1px solid #d0d7de;margin:6px 0;"></div>
      <a href="https://github.com/ssh221">@ssh221</a>
    </td>
    <td align="center">
      <a href="https://github.com/thdus"><img src="https://github.com/thdus.png" width="100px;" alt=""/></a>
      <div style="width:100px;border-top:1px solid #d0d7de;margin:8px 0;"></div>
      <b>김소연</b>
      <div style="width:60px;border-top:1px solid #d0d7de;margin:6px 0;"></div>
      <a href="https://github.com/thdus">@thdus</a>
    </td>
  </tr>
</table>


---

## ⛳ 프로젝트 배경

🛡️ **이상거래 탐지(FDS)의 의무화와 중요성**

최근 금융권 및 가상자산 시장을 타겟으로 한 보안 사고가 급증함에 따라 이상거래탐지시스템(FDS) 도입이 자율에서 의무화로 강화되는 추세다. 그리고 보안 사고는 선제적 탐지와 신속한 초기 대응이 피해 규모를 결정짓는 핵심 요소이기 때문에 FDS의 중요성은 날이 갈수록 커지고 있다.

### [관련 뉴스]
<img width="600" height="300" alt="image" src="https://github.com/user-attachments/assets/ab2642f5-a6be-427d-bffe-1190e9d49ea5" />

출처: https://www.mt.co.kr/stock/2025/10/28/2025102816154943473

<img width="600" height="300" alt="image" src="https://github.com/user-attachments/assets/365b78c2-7989-43db-a924-1bed4ca0ddd4" />

출처: https://www.fntimes.com/html/view.php?ud=202509051500181832300bf52dd2_18

🕒 **자동화의 필요성**

보안 위협은 24시간 발생하지만 인력이 실시간으로 모든 로그를 모니터링하고 즉각 대응하기에는 현실적인 한계가 있다.

모니터링 공백: 물리적인 24/7 대응의 어려움

대응 속도: 탐지 후 차단까지 소요되는 골든 타임 확보의 필요성

---

## 💡 프로젝트 목표
이미 시장에는 훌륭한 FDS 솔루션들이 존재하지만, 본 프로젝트에서는 워크플로우 자동화 도구인 n8n을 활용해 FDS의 핵심 프로세스를 직접 설계 및 구현해서 아래의 역량을 키우는 것을 목표로 하였다.

- **FDS 메커니즘 이해**: 탐지-분석-대응으로 이어지는 로직을 직접 구현하며 시스템 이해도 제고

- **자동화 기술 숙달**: n8n을 활용해 복잡한 대응 과정을 효율적으로 구축하는 역량 습득

---

## 🗃️ 시스템 아키텍처

<img width="1519" height="765" alt="image" src="https://github.com/user-attachments/assets/c5d11ebc-d3e0-41bd-832f-bfb4f1467306" />


### 구성 요소 역할

| 구성 요소 | 역할 |
| --- | --- |
| Java(Spring Boot) | 로그인/송금 기능, 이벤트 생성 |
| n8n | 이벤트 수집, 점수화, 판단, 대응 자동화 |
| Google Sheets | 룰셋(조건/점수) 관리 |
| ELK 스택 | 로그 저장 및 시각화 |
| Slack | 관리자 알림 |
| AI Agent | 상황 요약 보고서 작성 |

---

## 🛠️ 기술 스택 (Tech Stack)

### Backend

- Java 17
- Spring Boot
- REST API

### Automation

- n8n

### Storage

- Google Sheets (Rule Management)
- Redis / Elasticsearch (+Kibana)

### Notification

- Slack Webhook

---

## 🧩 주요 기능

### 💳 금융 기능 시뮬레이션

- 로그인
- 송금

### 📡 로그 기반 이벤트 수집

- 로그인 성공/실패
- 송금 요청
- IP, 접속 국가, 금액, 시간 정보 포함
  
  예시 이벤트 로그:
  ```json
  {
    "ts": "2026-01-11T18:06:30+09:00",
    "event_type": "TRANSFER",
    "event_id": "0c0f6e5c-0f7c-4b3d-8f2f-8b8df7a4b9d1",
    "user_id": "user_01",
    "result": "SUCCESS",
    "src_ip": "203.0.113.10",
    "country": "US",
    "hour": 18,
    "amount": 5500000,
    "to_bank": "Woori",
    "to_account_id": "110-***-1234"
  }

  ```

### 🧮 룰 기반 점수화 (Rule-based Scoring)

- 룰은 **Google Sheets**에서 관리
- n8n이 룰을 조회하여 점수 계산

  예시 룰:
  
  | Rule_ID | Category | Rule_Name | 조건 설명 | Target Field | Operator | Threshold | Score |
  |--------|----------|-----------|-----------|--------------|----------|-----------|-------|
  | R001 | LOGIN | 해외 로그인 | 국내(KR) 외 국가에서 로그인 발생 | country | != | KR | +40 |
  | R002 | TRANSFER | 고액 송금 | 기준 금액 이상 송금 요청 | amount | >= | 5,000,000 | +30 |
  | R003 | LOGIN | 야간 로그인 | 심야 시간대 로그인 | hour | BETWEEN | 0 ~ 5 | +20 |
  | R004 | TRANSFER | 단시간 다회 송금 | 짧은 시간 내 송금 횟수 초과 | tx_count | > | 5 | +50 |
  
  사용자별 위험 점수 집계 현황:
  
  | User_ID | Current_Total_Score | Last_Update_Time | Triggered_Rules | blocked |
  |--------|---------------------|------------------|-----------------|---| 
  | user_01 | 70 | 2026-01-11 04:06:30 +09:00 | 야간 로그인, 단시간 다회 송금 |true|
  | user_02 | 20 | 2025-05-20 13:50:20 +09:00 | 야간 로그인 |false|


### 🚨 이상금융거래 대응 자동화

| 위험도 | 대응 |
| --- | --- |
| LOW | 로그 저장 |
| MEDIUM | 로그 저장 + 추가 인증 |
| HIGH | 로그 저장 + Slack 알림 + 거래 차단 |


### 🗂️ 이상 로그 시각화

- Kibana로 로그의 이상치 시각화
- 관리자가 빠르게 확인 가능
<img width="1801" height="409" alt="Image" src="https://github.com/user-attachments/assets/54c2980b-2b6e-45ad-a46b-5f0d524383a6" />

---
## 🎯 n8n 워크플로우
<!-- n8n 워크플로우 설명-->

## 🔁 n8n 활용 포인트 (Why n8n?)

본 프로젝트에서 **n8n은 단순 알림 도구가 아니라 중앙 판단 엔진** 역할을 수행한다.

<img width="960" height="774" alt="image" src="https://github.com/user-attachments/assets/8410b8a7-a0fe-4349-9609-60ef4db36592" />


<img width="1430" height="599" alt="Image" src="https://github.com/user-attachments/assets/5b9835c0-9535-4cd7-aa64-745a14b8d5b1" />

---

## 🚧 트러블 슈팅
- workflow에서 switch 노드를 여러 번 반복하는 문제가 발생하였었다
  - 원인을 분석해보니 카테고리 명으로 분류되어 들어오는 input값이 2개여서 각각에 대해 동작을 수행하여 output이 2개로 나가고 있었다 
  - 이를 해결하기 위해 merge 노드를 추가하여 하나로 모아주는 작업을 진행하였다
- 키가 포함된 파일명을 변경하여 git.ignore에서 제외된 줄 모르고 commit을 했었다 
  - 개인 정보가 github에 push될 뻔하였지만 github 시스템 덕분에 push가 안됐었다

---

## 🔍 향후 개선 방향

1️⃣ **사후 탐지 -> 사전 차단**

- 현재는 트랜잭션 발생 이후 로그를 기반으로 이상 거래를 탐지하는 구조이기 때문에 이미 완료된 송금에 대해서는
사전 차단이 어렵다는 한계 존재
- 향후에는 송금 트랜잭션이 완료되기 전에 이상 여부를 판단하여 선제적으로 대응하는 구조로 확장 가능

2️⃣ **AI 보조 판단 도입**

- 룰 기반의 판단은 사용자별 거래 패턴을 충분히 고려하지 못하는 문제
- 향후에는 더 정교한 이상 거래 탐지를 위해 기존의 룰 기반에 AI 기반 판단을 추가하는 구조로 확장 가능

3️⃣ **룰 기반 -> 머신러닝 학습**
- 현재는 룰을 기반으로 이상 패턴들을 탐지하고 있는데 이런 정적인 규칙으로 이상 패턴들을 탐지하는 데에는 한계가 존재함
- 대량의 데이터가 라벨 데이터가 존재한다면 모델을 학습시킨 후 추론을 통해 이상치를 탐지하는 방법으로 확장 가능


