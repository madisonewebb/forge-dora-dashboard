resource "aws_db_subnet_group" "main" {
  name       = "dora-db-subnet-group"
  subnet_ids = aws_subnet.private[*].id

  tags = { Name = "dora-db-subnet-group" }
}

resource "aws_db_instance" "main" {
  identifier        = "dora-postgres"
  engine            = "postgres"
  engine_version    = "16"
  instance_class    = var.db_instance_class
  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = "dora"
  username = "dora"
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  # Automatic backups — 7-day retention
  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "Mon:04:00-Mon:05:00"

  # Enhanced monitoring
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.rds_monitoring.arn

  # Protect against accidental deletion
  deletion_protection = true
  skip_final_snapshot = false
  final_snapshot_identifier = "dora-postgres-final"

  tags = { Name = "dora-postgres" }
}

# IAM role for RDS Enhanced Monitoring
data "aws_iam_policy_document" "rds_monitoring_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["monitoring.rds.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "rds_monitoring" {
  name               = "dora-rds-monitoring-role"
  assume_role_policy = data.aws_iam_policy_document.rds_monitoring_assume.json
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}
