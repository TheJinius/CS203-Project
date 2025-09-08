# RDS Instance
resource "aws_db_instance" "postgres" {
  identifier              = "cs203-postgres-db"
  allocated_storage       = 20
  max_allocated_storage   = 20 # cap growth at free tier
  engine                  = "postgres"
  engine_version          = "15.5"            # stick to Postgres 15.x
  instance_class          = "db.t3.micro"     # free tier eligible
  username                = var.db_username
  password                = var.db_password
  db_name                 = "cs203db"
  parameter_group_name    = "default.postgres15"
  publicly_accessible     = true
  vpc_security_group_ids  = [var.rds_sg_id]
  db_subnet_group_name = var.rds_subnet_group_name
  skip_final_snapshot     = true
  enabled_cloudwatch_logs_exports = ["general", "error", "slowquery"]
  apply_immediately = true

  tags = {
    Name = "cs203-postgres-db"
    Project = "cs203"
  }
}