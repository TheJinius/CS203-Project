output "vpc_id" {
    value = aws_vpc.vpc.id
}

output "subnet_id" {
    value = [aws_subnet.private_rds_subnet_1.id, aws_subnet.private_rds_subnet_1.id]
}