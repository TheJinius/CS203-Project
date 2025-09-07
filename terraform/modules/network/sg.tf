resource "aws_security_group" "tariffs_db_security_group" {
  name_prefix = "tariffs-db-sg-dev"
  vpc_id      = var.vpc_id
  description = "Security group for tariffs RDS database"

  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = var.allowed_cidr_blocks
    description = "postgres allowed cidr blocks"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = {
    Name        = "tariffs-db-security-group-dev"
    Environment = var.Environment
  }

  lifecycle {
    create_before_destroy = true
  }
}