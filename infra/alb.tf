# alb.tf

# 1. 로드 밸런서 본체 생성
resource "aws_lb" "main" {
  name               = "token-ledger-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = [aws_subnet.public_a.id, aws_subnet.public_c.id]

  tags = { Name = "token-ledger-alb" }
}

# 2. 스프링 부트(8080)용 타겟 그룹
resource "aws_lb_target_group" "spring_tg" {
  name        = "tg-token-ledger-spring"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip" # Fargate 모드에서는 반드시 ip로 지정해야 합니다.

  health_check {
    path                = "/" # 스프링 부트에 헬스체크용 경로가 있다면 변경 가능
    port                = "8080"
    protocol            = "HTTP"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
}

# 3. 그라파나(3000)용 타겟 그룹
resource "aws_lb_target_group" "grafana_tg" {
  name        = "tg-token-ledger-grafana"
  port        = 3000
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/api/health" # 그라파나 자체 내장 헬스체크 경로
    port                = "3000"
    protocol            = "HTTP"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
}

# 4. 외부 80 포트 리스너 -> 스프링 부트(8080) 타겟 그룹으로 전달
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.spring_tg.arn
  }
}

# 5. 외부 3000 포트 리스너 -> 그라파나(3000) 타겟 그룹으로 전달
resource "aws_lb_listener" "grafana" {
  load_balancer_arn = aws_lb.main.arn
  port              = "3000"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grafana_tg.arn
  }
}

# 출력 파일: 생성이 완료되면 고정된 도메인 주소를 터미널에 띄워줍니다.
output "alb_dns_name" {
  value       = aws_lb.main.dns_name
  description = "The 고정 접속 URL 주소"
}