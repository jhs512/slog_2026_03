# 인프라 이해 노트

이 문서는 `infra/` 디렉토리를 빠르게 이해하기 위한 강의형 README다.  
설명은 실제 Terraform 코드 기준으로 진행하고, EC2 부트스트랩과 배포 파이프라인까지 함께 본다.

대상 독자:

- Terraform 기본 문법을 아는 사람
- AWS VPC / EC2 / Security Group 개념을 아는 사람
- 이 프로젝트 인프라가 "어떤 리소스를 만들고 왜 그렇게 구성했는지" 빠르게 파악하고 싶은 사람

---

## 강의 구성

1. 이 인프라를 한 문장으로 요약하면
2. `main.tf` 하나로 보는 전체 구조
3. 네트워크 — VPC, Subnet, Internet Gateway
4. 보안 — Security Group, IAM Role, Instance Profile
5. EC2 — 단일 인스턴스와 태그 전략
6. `user_data` — 서버가 처음 켜질 때 하는 일
7. secrets.tf — 무엇을 직접 넣어야 하는가
8. Terraform 실행 흐름
9. 이 인프라에서 배울 수 있는 것

---

# 1강. 이 인프라를 한 문장으로 요약하면

이 인프라는:

> Terraform으로 AWS 네트워크와 단일 EC2를 만들고, 그 EC2가 첫 부팅 시 Docker 기반 NPMplus / Redis / PostgreSQL 환경을 스스로 준비하게 하는 구조다.

조금 더 실무적으로 말하면:

- 배포 단위는 단일 EC2 1대다.
- AWS 자원 생성은 Terraform이 맡고
- 애플리케이션 실행 기반 준비는 `user_data` 셸 스크립트가 맡는다.
- EC2 안에서는 Docker 네트워크 `common` 위에 프록시, 캐시, DB를 컨테이너로 올린다.
- 이후 애플리케이션 배포는 GitHub Actions + SSM이 담당한다.

---

# 2강. `main.tf` 하나로 보는 전체 구조

핵심 리소스는 [`infra/main.tf`](C:/Users/jangk/IdeaProjects/slog_2026_03/infra/main.tf)에 거의 다 들어 있다.

구조를 크게 나누면:

```text
AWS
├── VPC 1개
├── Public Subnet 4개
├── Internet Gateway 1개
├── Route Table 1개
├── Security Group 1개
├── IAM Role / Instance Profile 1세트
└── EC2 1대
     └── user_data 부트스트랩
          ├── Docker 설치
          ├── common 네트워크 생성
          ├── npm_1
          ├── redis_1
          └── pg_1
```

즉 Terraform은 “리소스 생성”까지만 하지 않고, **EC2가 실제 서비스 준비를 마칠 때까지의 초기 부트스트랩**도 함께 정의한다.

---

# 3강. 네트워크 — VPC, Subnet, Internet Gateway

네트워크는 비교적 단순하다.

```hcl
resource "aws_vpc" "vpc_1" {
  cidr_block = "10.0.0.0/16"
}

resource "aws_subnet" "subnet_1" {
  cidr_block              = "10.0.0.0/24"
  map_public_ip_on_launch = true
}
```

읽는 포인트:

- VPC는 `10.0.0.0/16`
- 서브넷은 4개 (`10.0.0.0/24`, `10.0.1.0/24`, `10.0.2.0/24`, `10.0.3.0/24`)
- 전부 `map_public_ip_on_launch = true` 인 퍼블릭 서브넷
- Route Table은 `0.0.0.0/0 -> Internet Gateway`

즉 이 인프라는 private subnet / NAT gateway / multi-tier 분리까지 가진 구조는 아니다.  
학습과 운영 단순화를 위해 **퍼블릭 서브넷 + 단일 진입 서버** 구조를 택했다.

---

# 4강. 보안 — Security Group, IAM Role, Instance Profile

Security Group은 현재 단순하다.

```hcl
resource "aws_security_group" "sg_1" {
  ingress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
```

이건 운영 편의성은 높지만 보안은 약하다. 문서로는 반드시 이렇게 읽어야 한다.

- 지금은 학습/소규모 운영 중심 설정
- 실무라면 포트 제한과 출처 제한을 더 강하게 걸어야 함

IAM은 두 권한이 핵심이다.

- `AmazonSSMManagedInstanceCore`
  - GitHub Actions가 SSM으로 원격 명령을 내리기 위해 필요
- `AmazonS3FullAccess`
  - 현재 코드 기준으로 부여돼 있음

즉 EC2에 SSH로 직접 붙는 대신, **SSM이 표준 원격 실행 경로**가 되도록 설계했다.

---

# 5강. EC2 — 단일 인스턴스와 태그 전략

EC2는 1대만 만든다.

```hcl
resource "aws_instance" "ec2_1" {
  ami                    = data.aws_ssm_parameter.amazon_linux_ami.value
  instance_type          = "t3.micro"
  subnet_id              = aws_subnet.subnet_2.id
  vpc_security_group_ids = [aws_security_group.sg_1.id]

  tags = {
    Name = "${var.prefix}-ec2-1"
  }
}
```

핵심 포인트:

- 인스턴스 타입: `t3.micro`
- 배치 위치: `subnet_2`
- Name 태그: `${prefix}-ec2-1`

이 Name 태그가 중요하다.  
GitHub Actions 배포 워크플로우는 이 태그로 배포 대상을 찾는다.

즉 인프라와 배포 파이프라인이 **태그 명명 규칙**으로 연결된다.

---

# 6강. `user_data` — 서버가 처음 켜질 때 하는 일

이 인프라의 진짜 핵심은 `locals.ec2_bootstrap` 이다.

서버가 처음 켜지면 이 스크립트가 순서대로 실행된다.

## 1. 기본 OS 준비

- 타임존 `Asia/Seoul`
- `dnf update`
- `git`, `docker` 설치
- Docker 서비스 enable + start

## 2. Swap 설정

```bash
dd if=/dev/zero of=/swapfile bs=128M count=32
mkswap /swapfile
swapon /swapfile
```

`t3.micro` 메모리 제약을 완화하려는 의도다.

## 3. 환경 변수 기록

```bash
echo 'PASSWORD_1=...' >> /etc/environment
echo 'APP_1_DOMAIN=...' >> /etc/environment
echo 'APP_1_DB_NAME=...' >> /etc/environment
echo 'GITHUB_ACCESS_TOKEN_1_OWNER=...' >> /etc/environment
echo 'GITHUB_ACCESS_TOKEN_1=...' >> /etc/environment
```

이 값들은 나중에:

- NPMplus 로그인
- 데이터베이스 이름
- GHCR 로그인
- GitHub Actions 배포 스크립트

에 다시 쓰인다.

## 4. Docker 네트워크와 필수 컨테이너

```bash
docker network create common
```

이후 세 컨테이너를 올린다.

- `npm_1`
  - NPMplus
  - 80, 443, 81 포트 사용
- `redis_1`
  - Redis
  - 비밀번호 보호
- `pg_1`
  - `jangka512/pgj:latest`
  - PGroonga 포함 PostgreSQL 이미지

즉 이 인프라는 RDS / ElastiCache를 쓰지 않고, **EC2 내부 Docker 컨테이너로 인프라 서비스를 같이 운영**하는 구조다.

---

# 7강. `secrets.tf` — 무엇을 직접 넣어야 하는가

저장소에 있는 기본 파일은 [`infra/secrets.tf.default`](C:/Users/jangk/IdeaProjects/slog_2026_03/infra/secrets.tf.default)다.

직접 만드는 파일은 `secrets.tf` 다.

최소로 채워야 하는 값:

- `password_1`
- `app_1_db_name`
- `github_access_token_1_owner`
- `github_access_token_1`

예를 들어:

```hcl
variable "password_1" {
  default = "realpassword1234"
}

variable "github_access_token_1_owner" {
  default = "NEED_TO_INPUT"
}
```

주의:

- `secrets.tf` 는 추적 대상이 아니다
- `terraform.tfstate`, `terraform.tfstate.backup` 도 추적 대상이 아니다
- [`infra/.gitignore`](C:/Users/jangk/IdeaProjects/slog_2026_03/infra/.gitignore)에서 실제로 막고 있다

---

# 8강. Terraform 실행 흐름

실행 순서는 단순하다.

```bash
cd infra
cp secrets.tf.default secrets.tf
terraform init
terraform plan
terraform apply
```

각 단계 의미:

- `init`
  - provider 다운로드
- `plan`
  - 무엇이 만들어질지 미리보기
- `apply`
  - 실제 반영

이 인프라는 apply 후 끝이 아니라, **EC2가 부팅되면서 user_data까지 모두 끝나야 실제 배포 준비가 완료**된다.

즉 Terraform 성공 직후에도:

- Docker 설치 완료 여부
- `npm_1`, `redis_1`, `pg_1` 기동 여부
- `/etc/environment` 기록 여부

를 함께 봐야 한다.

---

# 9강. 이 인프라에서 배울 수 있는 것

- Terraform으로 AWS 기본 리소스 조합하기
- EC2 단일 서버 구조에서 운영 단순성 확보하기
- `user_data`로 서버 초기 상태를 코드화하기
- Docker 네트워크 위에 프록시/캐시/DB를 같이 올리는 구조
- GitHub Actions + SSM과 연결되는 태그/환경변수 전략

---

## 결론

이 인프라는 거대한 클라우드 아키텍처가 아니다. 대신 **단일 서버 기반 운영 단순성, 코드화된 부트스트랩, 배포 자동화 연결**에 초점을 둔 구조다.

읽을 때는:

1. Terraform이 무엇을 만드는가
2. `user_data`가 무엇을 설치하는가
3. GitHub Actions가 이 인프라를 어떻게 찾고 쓰는가

이 세 가지를 먼저 보면 전체 구조가 빠르게 잡힌다.
