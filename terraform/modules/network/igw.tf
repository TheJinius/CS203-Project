resource "aws_internet_gateway" "igw" {
    vpc_id = var.vpc_id
    tags = {
        name = "tariff-igw"
    }
}

#route table
resource "aws_route_table" "public_rt" {
    vpc_id = var.vpc_id
    route {
        cidr_block = "0.0.0.0/0"
        gateway_id = aws_internet_gateway.igw.id
    }

    tags = {
        Name = "project-public-rt"
    }
}

# Associate subnets with public route table
resource "aws_route_table_association" "public_assoc_1" {
  subnet_id      = aws_subnet.public_rds_subnet_1.id
  route_table_id = aws_route_table.public_rt.id
}

resource "aws_route_table_association" "public_assoc_2" {
  subnet_id      = aws_subnet.public_rds_subnet_2.id
  route_table_id = aws_route_table.public_rt.id
}