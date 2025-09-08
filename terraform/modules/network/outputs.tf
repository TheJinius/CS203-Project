output "vpc_id" {
    value = aws_vpc.vpc.id
}

output "subnet_ids" {
    value = [aws_subnet.public_rds_subnet_1.id, aws_subnet.public_rds_subnet_2.id]
}

output "rds_subnet_group_name" {
    value = aws_db_subnet_group.rds_public_subnet_group.name
}

output "rds_subnet_sg_ids" {
    value = aws_security_group.rds_sg.id
}