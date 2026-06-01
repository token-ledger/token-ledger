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
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
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
    path                = "/api/health"
    port                = "3000"
    protocol            = "HTTP"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
}

# 🌟 [추가됨] 4. 이미 발급된 ACM 인증서 데이터 가져오기
data "aws_acm_certificate" "issued_cert" {
  domain   = "token-ledger.site" # 발급받으신 도메인 입력
  statuses = ["ISSUED"]
}

# 🌟 [수정됨] 5. 외부 80 포트 리스너 -> HTTPS(443)로 강제 리다이렉트
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# 6. 외부 443(HTTPS) 포트 리스너 -> 그라파나 타겟 그룹으로 변경!
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2016-08"
  certificate_arn   = data.aws_acm_certificate.issued_cert.arn

  default_action {
    type = "forward"
    # 🚨 여기를 spring_tg.arn 에서 grafana_tg.arn 으로 변경합니다.
    target_group_arn = aws_lb_target_group.grafana_tg.arn
  }
}

# 7. 외부 3000 포트 리스너 -> 그라파나(3000) 타겟 그룹으로 전달 (기존 유지)
resource "aws_lb_listener" "grafana" {
  load_balancer_arn = aws_lb.main.arn
  port              = "3000"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.grafana_tg.arn
  }
}

# 🌟 [신규 추가] 9. HTTPS 경로 기반 라우팅 규칙 (API는 스프링 부트로!)
resource "aws_lb_listener_rule" "api_routing" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.spring_tg.arn
  }

  condition {
    path_pattern {
      values = ["/test/*", "/api/*"]
    }
  }
}

# 8. 출력 파일
output "alb_dns_name" {
  value       = aws_lb.main.dns_name
  description = "The 고정 접속 URL 주소"
}