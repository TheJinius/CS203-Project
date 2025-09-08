resource "aws_security_group" "rds_sg" {
  name_prefix = "rds_sg"
  vpc_id      = var.vpc_id
  description = "Security group for tariffs RDS database"

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "All outbound traffic"
  }

  tags = {
    Name        = "rds-subnet-group"
    Environment = var.Environment
  }
}