# network.tf

# 1. VPC 생성 (우리의 전용 네트워크 공간)
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
  tags = { Name = "token-ledger-vpc" }
}

# 2. 퍼블릭 서브넷 2개 (가용영역 a, c에 쪼개서 서버를 이중화할 공간)
resource "aws_subnet" "public_a" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "ap-northeast-2a"
  map_public_ip_on_launch = true # 컨테이너에 퍼블릭 IP 자동 할당
  tags = { Name = "token-ledger-public-a" }
}

resource "aws_subnet" "public_c" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "ap-northeast-2c"
  map_public_ip_on_launch = true
  tags = { Name = "token-ledger-public-c" }
}

# 3. 인터넷 게이트웨이 & 라우팅 (외부 인터넷과 통신하게 해주는 문)
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id
}

resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
}

resource "aws_route_table_association" "a" {
  subnet_id      = aws_subnet.public_a.id
  route_table_id = aws_route_table.public_rt.id
}

resource "aws_route_table_association" "c" {
  subnet_id      = aws_subnet.public_c.id
  route_table_id = aws_route_table.public_rt.id
}

# 4. 보안 그룹 (방화벽: 스프링 부트용 8080 포트 열어주기)
resource "aws_security_group" "ecs_sg" {
  name        = "token-ledger-ecs-sg"
  description = "Allow inbound traffic for Spring Boot"
  vpc_id      = aws_vpc.main.id

  # 인바운드 규칙 (밖에서 들어오는 요청 허용: 8080 포트)
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"] # 전 세계 어디서든 접속 가능
  }

  # 인바운드 규칙 2 (그라파나용 3000 추가)
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 아웃바운드 규칙 (안에서 밖으로 나가는 요청 허용: 전체 오픈)
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}