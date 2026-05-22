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

# 🌟 [수정됨] 1. 문지기(ALB) 전용 보안 그룹: 외부 전 세계에 80, 443, 3000 포트 개방
resource "aws_security_group" "alb_sg" {
  name        = "token-ledger-alb-sg"
  description = "Allow public HTTP traffic to ALB"
  vpc_id      = aws_vpc.main.id

  # 기존 80 포트 (HTTP)
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 🌟 [새로 추가된 부분] 443 포트 (HTTPS 자물쇠 통로)
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # 기존 3000 포트 (그라파나)
  ingress {
    from_port   = 3000
    to_port     = 3000
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 2. 실제 서버(ECS) 보안 그룹: 외부 직접 접속 차단, 오직 ALB가 넘겨준 트래픽만 허용
resource "aws_security_group" "ecs_sg" {
  name        = "token-ledger-ecs-sg"
  description = "Allow inbound traffic ONLY from ALB"
  vpc_id      = aws_vpc.main.id

  # 스프링 부트용: 오직 ALB를 거쳐온 8080 트래픽만 통과
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id] # ⭐️ ALB 보안그룹 필터링
  }

  # 그라파나용: 오직 ALB를 거쳐온 3000 트래픽만 통과
  ingress {
    from_port       = 3000
    to_port         = 3000
    protocol        = "tcp"
    security_groups = [aws_security_group.alb_sg.id] # ⭐️ ALB 보안그룹 필터링
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}