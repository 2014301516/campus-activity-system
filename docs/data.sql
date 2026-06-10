USE campus_activity;

SET NAMES utf8mb4;

-- 测试用户 (密码均为 123456)
INSERT INTO users (username, password, real_name, student_id, phone, email, role) VALUES
('admin',       '$2b$12$RVATw.DRCEQKqn02neeVrum0RHeANOt2hOZ40uWc24Bdoq80dyTYu', '系统管理员', '000000', '13800000000', 'admin@campus.edu', 'admin'),
('organizer01', '$2b$12$RVATw.DRCEQKqn02neeVrum0RHeANOt2hOZ40uWc24Bdoq80dyTYu', '张老师',     'T001',   '13800000001', 'zhang@campus.edu', 'organizer'),
('organizer02', '$2b$12$RVATw.DRCEQKqn02neeVrum0RHeANOt2hOZ40uWc24Bdoq80dyTYu', '李老师',     'T002',   '13800000002', 'li@campus.edu', 'organizer'),
('student01',   '$2b$12$RVATw.DRCEQKqn02neeVrum0RHeANOt2hOZ40uWc24Bdoq80dyTYu', '王同学',     '2024001', '13800000003', 'wang@campus.edu', 'student'),
('student02',   '$2b$12$RVATw.DRCEQKqn02neeVrum0RHeANOt2hOZ40uWc24Bdoq80dyTYu', '赵同学',     '2024002', '13800000004', 'zhao@campus.edu', 'student'),
('student03',   '$2b$12$RVATw.DRCEQKqn02neeVrum0RHeANOt2hOZ40uWc24Bdoq80dyTYu', '刘同学',     '2024003', '13800000005', 'liu@campus.edu', 'student');

-- 活动分类
INSERT INTO categories (id, name, description) VALUES
(1, '学术讲座', '学术类讲座、研讨会、学术交流活动'),
(2, '文体活动', '文艺演出、体育比赛、文化节等活动'),
(3, '志愿服务', '社会公益、校园志愿服务类活动'),
(4, '学科竞赛', '各类学科竞赛、创新创业比赛'),
(5, '社团活动', '学生社团组织的各类活动');

-- 示例活动
INSERT INTO activities (id, title, description, category_id, organizer_id, location, start_time, end_time, max_participants, current_participants, status) VALUES
(1, '人工智能前沿技术讲座', '特邀清华大学李教授来校讲授AI最新发展趋势与ChatGPT技术解析', 1, 2, '学术报告厅A101', '2026-06-15 14:00:00', '2026-06-15 16:00:00', 200, 2, 'approved'),
(2, '校园十大歌手大赛', '一年一度的校园歌手大赛，展示你的音乐才华', 2, 2, '大学生活动中心', '2026-06-20 18:00:00', '2026-06-20 21:00:00', 100, 1, 'approved'),
(3, '图书馆志愿服务活动', '协助图书馆整理书籍、引导读者，可获得志愿时长4小时', 3, 3, '校图书馆', '2026-06-18 09:00:00', '2026-06-18 17:00:00', 30, 2, 'approved'),
(4, 'ACM程序设计校赛', '全国ACM程序设计大赛校内选拔赛，三人一组报名', 4, 3, '计算机学院楼302', '2026-06-25 08:00:00', '2026-06-25 17:00:00', 60, 0, 'pending'),
(5, '吉他社春季招新', '吉他社新学期招新活动，欢迎零基础同学加入', 5, 2, '社团活动室B203', '2026-06-16 12:00:00', '2026-06-16 14:00:00', 50, 0, 'draft');

-- 示例报名记录
INSERT INTO registrations (id, user_id, activity_id, status) VALUES
(1, 4, 1, 'registered'),
(2, 5, 1, 'registered'),
(3, 6, 2, 'registered'),
(4, 4, 3, 'registered'),
(5, 5, 3, 'registered');

-- 示例签到记录
INSERT INTO sign_ins (id, registration_id, user_id, activity_id, sign_in_time, sign_out_time) VALUES
(1, 1, 4, 1, '2026-06-15 13:55:00', '2026-06-15 16:05:00'),
(2, 2, 5, 1, '2026-06-15 14:00:00', NULL);

-- 示例评价
INSERT INTO reviews (user_id, activity_id, rating, comment) VALUES
(4, 1, 5, '讲座非常精彩，收获很大！'),
(5, 1, 4, '内容很好，就是时间有点短');

-- 示例公告
INSERT INTO notices (title, content, publisher_id) VALUES
('关于系统上线的通知', '校园活动管理系统已正式上线运行，欢迎各位同学和老师使用！', 1),
('活动报名须知', '请同学们在报名活动时注意活动时间和地点，报名后如需取消请在活动开始前24小时操作。', 1);
