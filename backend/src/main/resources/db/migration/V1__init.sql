-- Users
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Organizations
CREATE TABLE sys_organization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Organization members
CREATE TABLE sys_org_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organization_id) REFERENCES sys_organization(id),
    FOREIGN KEY (user_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_org_user (organization_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Folders
CREATE TABLE note_folder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(10) DEFAULT '📁',
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Notes
CREATE TABLE note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    folder_id BIGINT,
    title VARCHAR(255) NOT NULL DEFAULT '',
    content LONGTEXT,
    excerpt VARCHAR(500) DEFAULT '',
    is_pinned TINYINT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type),
    INDEX idx_folder (folder_id),
    FULLTEXT INDEX ft_search (title, excerpt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tags
CREATE TABLE note_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type),
    UNIQUE KEY uk_tenant_tag (tenant_id, tenant_type, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE note_tag_relation (
    note_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY (note_id, tag_id),
    FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES note_tag(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tasks
CREATE TABLE task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    note_id BIGINT,
    content VARCHAR(500) NOT NULL,
    completed TINYINT(1) DEFAULT 0,
    priority VARCHAR(10) DEFAULT 'MEDIUM',
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Daily notes
CREATE TABLE daily_note (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    note_date DATE NOT NULL,
    content LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_date (tenant_id, tenant_type, note_date),
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Calendar events
CREATE TABLE calendar_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    title VARCHAR(200) NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME,
    description VARCHAR(500),
    color VARCHAR(20) DEFAULT '#6366f1',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type),
    INDEX idx_date (event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Whiteboards
CREATE TABLE whiteboard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    tenant_type VARCHAR(20) NOT NULL DEFAULT 'PERSONAL',
    title VARCHAR(200) NOT NULL DEFAULT '新白板',
    data LONGTEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id, tenant_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
