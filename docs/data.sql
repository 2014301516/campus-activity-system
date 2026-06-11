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
INSERT INTO activities (id, title, description, cover_image, category_id, organizer_id, location, start_time, end_time, max_participants, current_participants, status) VALUES
(1, '人工智能前沿技术讲座', '特邀清华大学李教授来校讲授AI最新发展趋势与ChatGPT技术解析', 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80', 1, 2, '学术报告厅A101', '2026-06-15 14:00:00', '2026-06-15 16:00:00', 200, 2, 'approved'),
(2, '校园十大歌手大赛', '一年一度的校园歌手大赛，展示你的音乐才华', 'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=1200&q=80', 2, 2, '大学生活动中心', '2026-06-20 18:00:00', '2026-06-20 21:00:00', 100, 1, 'approved'),
(3, '图书馆志愿服务活动', '协助图书馆整理书籍、引导读者，可获得志愿时长4小时', 'https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?auto=format&fit=crop&w=1200&q=80', 3, 3, '校图书馆', '2026-06-18 09:00:00', '2026-06-18 17:00:00', 30, 2, 'approved'),
(4, 'ACM程序设计校赛', '全国ACM程序设计大赛校内选拔赛，三人一组报名', 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1200&q=80', 4, 3, '计算机学院楼302', '2026-06-25 08:00:00', '2026-06-25 17:00:00', 60, 0, 'pending'),
(5, '吉他社春季招新', '吉他社新学期招新活动，欢迎零基础同学加入', 'https://images.unsplash.com/photo-1510915361894-db8b60106cb1?auto=format&fit=crop&w=1200&q=80', 5, 2, '社团活动室B203', '2026-06-16 12:00:00', '2026-06-16 14:00:00', 50, 0, 'approved'),
(6, '前端开发实战工作坊', '围绕 Vue 3、组件封装和页面交互设计开展半天工作坊，适合想提升项目实战能力的同学参加。', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?auto=format&fit=crop&w=1200&q=80', 1, 2, '信息楼 402 实验室', '2026-06-11 14:00:00', '2026-06-11 17:00:00', 80, 0, 'approved'),
(7, '校园篮球友谊赛', '学院之间开展一场轻松的篮球友谊赛，现场设有观众互动和啦啦队加油环节。', 'https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=1200&q=80', 2, 3, '东操场篮球馆', '2026-06-12 18:30:00', '2026-06-12 20:30:00', 120, 0, 'approved'),
(9, '校园摄影采风活动', '摄影协会组织校园采风活动，带大家记录初夏校园风景并交流拍摄技巧。', 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80', 5, 2, '图书馆前广场', '2026-06-11 15:30:00', '2026-06-11 18:00:00', 40, 0, 'approved'),
(10, '创新创业项目路演分享', '邀请优秀创业团队进行项目路演与经验分享，帮助同学了解创新创业比赛和项目孵化流程。', 'https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1200&q=80', 4, 3, '大学生活动中心 201', '2026-06-12 19:00:00', '2026-06-12 21:30:00', 90, 0, 'approved'),
(11, '考研经验交流会', '邀请上岸学长学姐分享备考节奏、资料选择和复习误区，适合准备考研的同学提前了解规划。', 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80', 1, 2, '教学楼 C201', '2026-06-17 19:00:00', '2026-06-17 21:00:00', 150, 3, 'approved'),
(12, '校园荧光夜跑', '夜跑社组织环校园荧光夜跑，设置打卡点和补给站，适合演示多人参与的文体活动。', 'https://images.unsplash.com/photo-1486218119243-13883505764c?auto=format&fit=crop&w=1200&q=80', 2, 3, '西操场集合', '2026-06-19 19:30:00', '2026-06-19 21:00:00', 180, 56, 'approved'),
(13, '毕业季旧物捐赠志愿行动', '联合学生会开展旧书、文具和生活用品捐赠整理活动，参与可登记志愿服务时长。', 'https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&w=1200&q=80', 3, 2, '一食堂门前服务点', '2026-06-13 09:00:00', '2026-06-13 16:00:00', 60, 3, 'approved'),
(14, '数学建模赛前培训', '面向准备参加数学建模竞赛的同学开展建模思路、论文结构和软件工具使用培训。', 'https://images.unsplash.com/photo-1509228468518-180dd4864904?auto=format&fit=crop&w=1200&q=80', 4, 3, '理学院 305', '2026-06-22 14:00:00', '2026-06-22 17:30:00', 100, 32, 'approved'),
(15, '汉服文化体验日', '汉服社举办试穿、妆造展示和传统礼仪体验活动，现场可拍照打卡并参与互动问答。', 'https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=1200&q=80', 5, 2, '学生事务中心一楼大厅', '2026-06-21 13:30:00', '2026-06-21 17:00:00', 80, 3, 'approved'),
(16, '就业指导简历门诊', '邀请企业 HR 和辅导员现场修改简历、模拟提问，帮助毕业生优化求职材料。', 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1200&q=80', 1, 3, '招生就业处报告厅', '2026-06-11 10:00:00', '2026-06-11 18:00:00', 70, 43, 'ongoing'),
(17, '校园足球联赛决赛', '本学期校园足球联赛迎来决赛日，现场设有加油区、拍照区和颁奖环节。', 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1200&q=80', 2, 2, '南区足球场', '2026-06-10 16:00:00', '2026-06-10 18:00:00', 200, 3, 'ended'),
(18, '社区敬老志愿探访', '组织学生前往社区开展陪伴交流、卫生整理和智能手机使用辅导志愿服务。', 'https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?auto=format&fit=crop&w=1200&q=80', 3, 3, '校外社区服务站', '2026-06-24 08:30:00', '2026-06-24 12:00:00', 40, 0, 'pending'),
(19, '机器人创意挑战赛说明会', '为即将开始的机器人创意挑战赛做规则说明和答疑，帮助队伍提前准备选题方向。', 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1200&q=80', 4, 2, '工程训练中心 118', '2026-06-23 15:00:00', '2026-06-23 17:00:00', 90, 0, 'rejected'),
(20, '话剧社周末开放排练', '开放排练原计划面向全校旁听体验，因场地临时调整取消，保留记录用于后台演示。', 'https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1200&q=80', 5, 3, '艺术中心黑匣子剧场', '2026-06-14 14:00:00', '2026-06-14 17:00:00', 60, 0, 'cancelled'),
(21, '短视频创作训练营', '新媒体中心筹备中的系列训练营，当前仍在完善讲师安排和课程节奏，保留为待审核状态。', 'https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?auto=format&fit=crop&w=1200&q=80', 5, 2, '传媒实验室 204', '2026-06-28 14:00:00', '2026-06-28 17:30:00', 50, 0, 'pending'),
(22, 'Python 数据分析入门沙龙', '结合校园数据案例讲解 Python 数据清洗、可视化和基础分析流程，适合零基础同学。', 'https://images.unsplash.com/photo-1516116216624-53e697fedbea?auto=format&fit=crop&w=1200&q=80', 1, 3, '信息楼 305', '2026-06-26 14:00:00', '2026-06-26 16:30:00', 120, 39, 'approved'),
(23, '校园创客开放日', '面向全校开放创客空间，展示 3D 打印、单片机和机器人小项目，方便同学现场体验创新实践。', 'https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1200&q=80', 4, 2, '工程训练中心一楼', '2026-06-10 09:00:00', '2026-06-10 17:30:00', 120, 3, 'ended'),
(24, '毕业答辩经验分享会', '邀请优秀毕业生分享答辩准备、PPT 组织和现场表达技巧，适合近期准备汇报的同学参考。', 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80', 1, 3, '图书馆报告厅', '2026-06-10 14:30:00', '2026-06-11 17:30:00', 160, 3, 'ongoing'),
(25, '校园爱心义卖活动', '学生志愿者联合开展旧物义卖和公益募捐，现场设置摊位互动区和爱心留言墙。', 'https://images.unsplash.com/photo-1488521787991-ed7bbaae773c?auto=format&fit=crop&w=1200&q=80', 3, 2, '一食堂门前广场', '2026-06-10 10:00:00', '2026-06-10 18:00:00', 90, 3, 'ended'),
(26, '街舞社专场快闪表演', '街舞社在校园主干道开展快闪表演和即兴互动，现场气氛活跃，适合演示文体活动场景。', 'https://images.unsplash.com/photo-1504609773096-104ff2c73ba4?auto=format&fit=crop&w=1200&q=80', 2, 3, '中心广场', '2026-06-10 19:00:00', '2026-06-10 20:30:00', 150, 3, 'ended');

-- 示例报名记录
INSERT INTO registrations (id, user_id, activity_id, status) VALUES
(1, 4, 1, 'registered'),
(2, 5, 1, 'registered'),
(3, 6, 2, 'registered'),
(4, 4, 3, 'registered'),
(5, 5, 3, 'registered'),
(13, 4, 23, 'registered'),
(14, 5, 23, 'registered'),
(15, 6, 23, 'registered'),
(16, 4, 24, 'registered'),
(17, 5, 24, 'registered'),
(18, 6, 24, 'registered'),
(19, 4, 25, 'registered'),
(20, 5, 25, 'registered'),
(21, 6, 25, 'registered'),
(22, 4, 26, 'registered'),
(23, 5, 26, 'registered'),
(24, 6, 26, 'registered'),
(25, 4, 11, 'registered'),
(26, 5, 11, 'registered'),
(27, 6, 11, 'registered'),
(28, 4, 13, 'registered'),
(29, 5, 13, 'registered'),
(30, 6, 13, 'registered'),
(31, 4, 15, 'registered'),
(32, 5, 15, 'registered'),
(33, 6, 15, 'registered'),
(34, 4, 17, 'registered'),
(35, 5, 17, 'registered'),
(36, 6, 17, 'registered');

-- 示例签到记录
INSERT INTO sign_ins (id, registration_id, user_id, activity_id, sign_in_time, sign_out_time) VALUES
(1, 1, 4, 1, '2026-06-15 13:55:00', '2026-06-15 16:05:00'),
(2, 2, 5, 1, '2026-06-15 14:00:00', NULL),
(5, 13, 4, 23, '2026-06-10 09:12:00', '2026-06-10 16:58:00'),
(6, 14, 5, 23, '2026-06-10 09:20:00', '2026-06-10 17:05:00'),
(7, 16, 4, 24, '2026-06-10 14:28:00', NULL),
(8, 17, 5, 24, '2026-06-10 14:35:00', NULL),
(9, 19, 4, 25, '2026-06-10 10:08:00', '2026-06-10 17:42:00'),
(10, 22, 4, 26, '2026-06-10 18:52:00', '2026-06-10 20:31:00'),
(11, 34, 4, 17, '2026-06-10 15:55:00', '2026-06-10 18:02:00'),
(12, 35, 5, 17, '2026-06-10 16:01:00', '2026-06-10 18:04:00');

-- 示例评价
INSERT INTO reviews (user_id, activity_id, rating, comment) VALUES
(4, 1, 5, '讲座非常精彩，收获很大！'),
(5, 1, 4, '内容很好，就是时间有点短');

-- 示例公告
INSERT INTO notices (title, content, publisher_id) VALUES
('关于系统上线的通知', '校园活动管理系统已正式上线运行，欢迎各位同学和老师使用！', 1),
('活动报名须知', '请同学们在报名活动时注意活动时间和地点，报名后如需取消请在活动开始前24小时操作。', 1);
