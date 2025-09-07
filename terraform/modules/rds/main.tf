 resource "aws_db_instance" "postgres" {
  identifier = "cs203-postgres-db"
  allocated_storage = 20
  engine = "postgres"
  engine_version = "17.4-R1"
  instance_class = "db.t3.micro"
  username = var.db_username
  password = var.db_password
  db_name = "cs203db"
  parameter_group_name = "default.postgres15"
  publicly_accessible = true
  skip_final_snapshot = true
 }