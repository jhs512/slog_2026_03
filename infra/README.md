# infra

이 디렉토리는 서비스의 배포 및 실행 환경을 구축하기 위한 **Terraform (AWS) 인프라 구성 코드** 창고입니다.
백엔드 코드와 분리되어 있지만, 백엔드/프론트엔드를 호스팅할 최소한의 아키텍처를 원클릭에 가깝게 프로비저닝하는 역할을 합니다.

---

## 🏗️ 구성 아키텍처 개요

단일 EC2를 기반으로 하여 복잡성을 줄인 구조입니다.

- **네트워크 (VPC)**
  - VPC, Subnet, Internet Gateway 
  - 외부 통신을 위한 Route Table 매핑
- **보안 (Security Group & IAM)**
  - Security Group: 편의성을 위해 기본적으로 인터넷에 열려 있도록(`0.0.0.0/0`) 설정됩니다. (실무 서비스 운영시 반드시 수정 필요)
  - IAM Role & Instance Profile: EC2 셸 접근을 위한 `SSMManagedInstanceCore` 및 리소스 제어를 위한 `S3FullAccess` 권한 부여.
- **컴퓨팅 (EC2 Instance)**
  - 인스턴스 타입: 가성비가 높은 `t3.micro` 등
  - OS 이미지(AMI): `Amazon Linux 2023`

---

## ⚙️ EC2 부트스트랩 (User Data)

이 프로젝트의 인프라 핵심은 단순 인스턴스 생성이 아니라, EC2가 처음 기동(`Booting`)될 때 필요한 환경 설정과 도커 컨테이너들을 스스로 준비하도록 세팅된 `main.tf` 의 **`user_data` 셸 스크립트** 입니다.

부트스트랩 스크립트는 다음 작업을 백그라운드에서 자동 순차 실행합니다:
1. **타임존 설정**: 서버 기준 시간을 `Asia/Seoul` 로 변경
2. **패키지 설치**: `dnf`를 통해 `git`, `docker` 등 필수 유틸리티 및 컨테이너 엔진 세팅
3. **Swap 메모리 할당**: 마이크로 인스턴스의 메모리 한계를 우회하기 위해 4GB 의 가상 메모리 스왑 파일 세팅
4. **필수 컨테이너 기동**:
   - `npm_1 (NPMplus)`: 로컬의 트래픽을 처리할 오픈소스 Nginx Proxy Manager 구동
   - `redis_1`: 분산락 및 캐시, 세션 관리 지원
   - `pg_1 (PostgreSQL)`: 실제 데이터를 적재할 메인 RDBMS 구동
5. **GHCR 로그인**: 배포 시 프라이빗 이미지가 존재할 경우를 대비하여 `GitHub Container Registry` 자격 증명(토큰) 캐싱

이 작업이 완료되면 인스턴스는 GitHub Action(`deploy.yml`) 배포 대상이 될 준비 상태가 됩니다.

---

## 🚀 사용법 (프로비저닝하기)

이 저장소의 코드를 실제로 당신의 AWS 계정에 반영하려면 아래 순서를 따르세요.

### 1단계: 시크릿 설정 파일 준비
`secrets.tf.default` 파일의 내용을 참고하여 실제 민감 데이터가 담길 `secrets.tf` 를 생성합니다.
```bash
cp secrets.tf.default secrets.tf
```
*(`secrets.tf`는 파일 구조상 `.gitignore` 에 포함되어 있으므로 절대로 Git 원격 저장소에 올리지 않도록 주의하세요.)*

`secrets.tf`에 데이터베이스 비밀번호나 GitHub 토큰 등을 채워 넣으세요.

### 2단계: Terraform 실행

테라폼 CLI 도구가 설치되어 있어야 하며, 로컬 장비에 AWS 인증 자격 증명(`aws configure`)이 세팅되어 있어야 합니다.

```bash
# 워크스페이스 초기화 및 관련 플러그인 로드
terraform init

# 어떤 리소스가 변경/생성 될지 미리보기 
terraform plan

# 인프라 변경 실제 적용 대상에 반영 
terraform apply
```

적용이 완료되면 콘솔창에서 EC2의 Public IP 등을 확인할 수 있습니다.

---

## 📂 파일 트리 가이드

- `main.tf`: EC2, VPC, SG 등 Terraform의 실제 뼈대와 구성 스크립트(user_data) 정의.
- `variables.tf`: 프로젝트 내에서 반복 재사용될 기본 변수 선언.
- `secrets.tf`: (직접 생성) 노출되어서는 안되는 비밀번호 토큰 등의 민감 변수값.
- `terraform.tfstate` / `.backup`: 현재 인프라 구조를 기억하는 상태 저장 파일 (이 파일들도 `.gitignore` 에 안전하게 등록되어 있습니다).
