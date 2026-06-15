-- ============================================================
-- 古代天平衡器系统 - 新功能扩展SQL
-- 功能: 1.制造工艺反演 2.多文明对比 3.现代校准应用 4.虚拟称量体验
-- ============================================================

-- ============================================================
-- 功能一: 制造工艺反演分析表
-- ============================================================
CREATE TABLE IF NOT EXISTS manufacturing_analyses (
    id SERIAL PRIMARY KEY,
    balance_id INTEGER NOT NULL REFERENCES balances(id),
    analysis_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    knife_edge_geometry_score NUMERIC(5,2),
    surface_roughness_score NUMERIC(5,2),
    material_quality_score NUMERIC(5,2),
    assembly_precision_score NUMERIC(5,2),
    overall_technology_grade VARCHAR(20),
    estimated_manufacturing_era VARCHAR(50),
    inferred_craft_method VARCHAR(100),
    geometry_tolerance_microm NUMERIC(10,4),
    inferred_surface_roughness_ra NUMERIC(10,4),
    material_homogeneity NUMERIC(5,2),
    arm_length_ratio_error NUMERIC(10,6),
    raw_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_manufacturing_balance_id ON manufacturing_analyses(balance_id);
CREATE INDEX IF NOT EXISTS idx_manufacturing_grade ON manufacturing_analyses(overall_technology_grade);
CREATE INDEX IF NOT EXISTS idx_manufacturing_time ON manufacturing_analyses(analysis_time DESC);

-- ============================================================
-- 功能二: 多文明天平对比表
-- ============================================================
CREATE TABLE IF NOT EXISTS civilization_balances (
    id SERIAL PRIMARY KEY,
    civilization_name VARCHAR(50) NOT NULL,
    civilization_code VARCHAR(20) NOT NULL UNIQUE,
    period_start_year INTEGER,
    period_end_year INTEGER,
    balance_type VARCHAR(50),
    max_capacity NUMERIC(10,2),
    relative_precision NUMERIC(12,8),
    material_hardness NUMERIC(8,2),
    arm_ratio_consistency NUMERIC(5,2),
    structure_complexity NUMERIC(5,2),
    durability_score NUMERIC(5,2),
    typical_arm_length NUMERIC(8,2),
    typical_material VARCHAR(50),
    cultural_significance TEXT,
    representative_artifact VARCHAR(200),
    discovery_location VARCHAR(200),
    reference_source TEXT,
    radar_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_civilization_code ON civilization_balances(civilization_code);
CREATE INDEX IF NOT EXISTS idx_civilization_period ON civilization_balances(period_start_year);

-- ============================================================
-- 初始化多文明天平数据
-- ============================================================
INSERT INTO civilization_balances (
    civilization_name, civilization_code, period_start_year, period_end_year,
    balance_type, max_capacity, relative_precision, material_hardness,
    arm_ratio_consistency, structure_complexity, durability_score,
    typical_arm_length, typical_material, cultural_significance,
    representative_artifact, discovery_location, reference_source, radar_data
) VALUES
(
    '战国-中国', 'CHN-WARRING', -475, -221,
    'EQUAL_ARM', 500.00, 0.0001, 150.0, 95.0, 70.0, 85.0,
    150.0, '青铜', '战国时期各国度量衡不统一，秦楚齐等国已有等臂天平',
    '战国铜权、天平铜钩', '河南安阳、湖南长沙', '《中国科学技术史·度量衡卷》',
    '{"maxCapacity":80,"relativePrecision":90,"materialHardness":75,"armRatioConsistency":95,"structureComplexity":70,"durabilityScore":85}'
),
(
    '古罗马', 'ROME', -300, 476,
    'UNEQUAL_ARM', 300.00, 0.0005, 180.0, 80.0, 85.0, 75.0,
    120.0, '青铜', '罗马天平(Steelyard)为不等臂，使用游标原理，适合商业贸易',
    '罗马铜杆秤、庞贝遗址出土天平', '意大利庞贝、罗马', '《Roman Weights and Measures》',
    '{"maxCapacity":60,"relativePrecision":70,"materialHardness":90,"armRatioConsistency":80,"structureComplexity":85,"durabilityScore":75}'
),
(
    '古埃及', 'EGYPT', -2500, -30,
    'EQUAL_ARM', 200.00, 0.0002, 120.0, 90.0, 60.0, 70.0,
    100.0, '石灰岩、铜', '埃及天平用于称量香料、贵金属，悬垂式等臂设计',
    '图坦卡蒙墓出土金天平、莎草纸壁画', '埃及卢克索、吉萨', '《Ancient Egyptian Weighing Instruments》',
    '{"maxCapacity":40,"relativePrecision":85,"materialHardness":60,"armRatioConsistency":90,"structureComplexity":60,"durabilityScore":70}'
),
(
    '古希腊', 'GREECE', -800, -146,
    'EQUAL_ARM', 150.00, 0.0003, 140.0, 88.0, 75.0, 72.0,
    110.0, '青铜、大理石', '希腊天平用于医药和珠宝称量，精密程度较高',
    '雅典集市出土天平、希波克拉底医药天平', '希腊雅典、克里特', '《Ancient Greek Precision Instruments》',
    '{"maxCapacity":30,"relativePrecision":80,"materialHardness":70,"armRatioConsistency":88,"structureComplexity":75,"durabilityScore":72}'
),
(
    '古巴比伦', 'BABYLON', -2000, -539,
    'EQUAL_ARM', 250.00, 0.0004, 130.0, 85.0, 65.0, 68.0,
    95.0, '青铜、玄武岩', '巴比伦天平用于商业税收和贡赋称量',
    '汉谟拉比法典记载权衡、尼尼微遗址出土权器', '伊拉克巴比伦、尼尼微', '《Mesopotamian Metrology》',
    '{"maxCapacity":50,"relativePrecision":75,"materialHardness":65,"armRatioConsistency":85,"structureComplexity":65,"durabilityScore":68}'
),
(
    '古印度', 'INDIA', -600, 500,
    'EQUAL_ARM', 180.00, 0.00025, 145.0, 92.0, 72.0, 78.0,
    105.0, '青铜', '印度河流域文明已有精密权衡，孔雀王朝时期完善',
    '塔克西拉出土青铜天平、孔雀王朝标准权', '巴基斯坦塔克西拉、印度巴特那', '《Indian Metrology Through the Ages》',
    '{"maxCapacity":36,"relativePrecision":83,"materialHardness":72,"armRatioConsistency":92,"structureComplexity":72,"durabilityScore":78}'
),
(
    '西汉-中国', 'CHN-WEST-HAN', -202, 8,
    'EQUAL_ARM', 600.00, 0.00008, 150.0, 97.0, 75.0, 88.0,
    160.0, '青铜', '西汉度量衡统一，满城汉墓出土精度极高的铜权',
    '满城汉墓铜权、马王堆汉墓天平衡杆', '河北满城、湖南长沙', '《西汉度量衡》',
    '{"maxCapacity":85,"relativePrecision":92,"materialHardness":75,"armRatioConsistency":97,"structureComplexity":75,"durabilityScore":88}'
),
(
    '唐代-中国', 'CHN-TANG', 618, 907,
    'EQUAL_ARM', 800.00, 0.00006, 155.0, 98.0, 80.0, 90.0,
    170.0, '青铜、钢', '唐代度量衡制度完备，戥秤发明，金银珠宝精密称量',
    '唐代银铤、铜权、敦煌壁画称量图', '陕西西安、甘肃敦煌', '《唐代度量衡研究》',
    '{"maxCapacity":90,"relativePrecision":95,"materialHardness":78,"armRatioConsistency":98,"structureComplexity":80,"durabilityScore":90}'
),
(
    '中世纪伊斯兰', 'ISLAMIC', 700, 1500,
    'UNEQUAL_ARM', 400.00, 0.0003, 170.0, 82.0, 90.0, 80.0,
    130.0, '黄铜、钢', '伊斯兰Qabbalun天平，带精细刻度的不等臂设计，科技领先',
    '阿拔斯王朝黄铜天平、《精密仪器论》', '伊拉克巴格达、埃及开罗', '《Islamic Scientific Instruments》',
    '{"maxCapacity":70,"relativePrecision":80,"materialHardness":85,"armRatioConsistency":82,"structureComplexity":90,"durabilityScore":80}'
),
(
    '中世纪欧洲', 'EUROPE-MED', 500, 1500,
    'UNEQUAL_ARM', 350.00, 0.0006, 160.0, 78.0, 78.0, 72.0,
    115.0, '铁、黄铜', '中世纪欧洲商用杆秤，工艺较粗糙，精度一般',
    '伦敦考古出土中世纪杆秤、汉萨同盟标准权', '英国伦敦、德国吕贝克', '《Medieval European Weighing》',
    '{"maxCapacity":65,"relativePrecision":68,"materialHardness":80,"armRatioConsistency":78,"structureComplexity":78,"durabilityScore":72}'
),
(
    '文艺复兴-欧洲', 'EUROPE-RENAISS', 1400, 1700,
    'EQUAL_ARM', 500.00, 0.00004, 200.0, 99.0, 95.0, 92.0,
    180.0, '钢、玛瑙', '文艺复兴时期精密分析天平，玛瑙刀口，开始科学计量',
    '伽利略设计天平、皇家学会标准天平', '意大利佛罗伦萨、英国伦敦', '《The Balance and the Mirror》',
    '{"maxCapacity":80,"relativePrecision":98,"materialHardness":100,"armRatioConsistency":99,"structureComplexity":95,"durabilityScore":92}'
),
(
    '明代-中国', 'CHN-MING', 1368, 1644,
    'EQUAL_ARM', 700.00, 0.00007, 160.0, 97.5, 82.0, 89.0,
    165.0, '铜、钢', '明代戥秤工艺达到高峰，万历年间标准权器',
    '万历铜权、明代戥秤、《天工开物》记载', '北京、湖北钟祥', '《明代度量衡》',
    '{"maxCapacity":88,"relativePrecision":93,"materialHardness":80,"armRatioConsistency":97,"structureComplexity":82,"durabilityScore":89}'
)
ON CONFLICT (civilization_code) DO NOTHING;

-- ============================================================
-- 功能三: 现代天平校准应用
-- ============================================================
CREATE TABLE IF NOT EXISTS calibration_devices (
    id SERIAL PRIMARY KEY,
    device_code VARCHAR(50) NOT NULL UNIQUE,
    device_name VARCHAR(100) NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    balance_type VARCHAR(20) DEFAULT 'EQUAL_ARM',
    left_arm_length NUMERIC(10,4),
    right_arm_length NUMERIC(10,4),
    fulcrum_position NUMERIC(10,4),
    knife_edge_radius NUMERIC(10,6),
    max_capacity NUMERIC(10,2),
    min_readability NUMERIC(12,8),
    material VARCHAR(50),
    description TEXT,
    calibration_protocol JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS calibration_results (
    id SERIAL PRIMARY KEY,
    device_id INTEGER REFERENCES calibration_devices(id),
    balance_id INTEGER REFERENCES balances(id),
    calibration_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    calibration_method VARCHAR(50),
    left_arm_correction NUMERIC(12,8),
    right_arm_correction NUMERIC(12,8),
    arm_length_ratio_correction NUMERIC(12,8),
    zero_point_drift NUMERIC(12,8),
    linearity_error NUMERIC(12,8),
    repeatability_std NUMERIC(12,8),
    hysteresis_error NUMERIC(12,8),
    corrected_uncertainty NUMERIC(12,8),
    calibration_grade VARCHAR(20),
    positions_data JSONB,
    correction_table JSONB,
    raw_measurements JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_calibration_device ON calibration_results(device_id);
CREATE INDEX IF NOT EXISTS idx_calibration_balance ON calibration_results(balance_id);
CREATE INDEX IF NOT EXISTS idx_calibration_time ON calibration_results(calibration_time DESC);

-- ============================================================
-- 初始化校准装置数据
-- ============================================================
INSERT INTO calibration_devices (
    device_code, device_name, device_type, balance_type,
    left_arm_length, right_arm_length, fulcrum_position,
    knife_edge_radius, max_capacity, min_readability,
    material, description, calibration_protocol
) VALUES
(
    'CAL-LEVER-001', '古代杠杆原理简易校准装置', 'LEVER_PRINCIPLE', 'EQUAL_ARM',
    200.0000, 200.0000, 0.0000,
    0.500000, 1000.00, 0.0001,
    '6061铝合金+玛瑙刀口', '基于中国古代等臂杠杆原理的现代校准装置，支持多位置校准',
    '{"steps":["空载调零","左盘100g校准","右盘100g校准","交换位置复校","50%负载校准","100%负载校准"],"positions":["center","left1","left2","right1","right2"],"tolerance":0.0001}'
),
(
    'CAL-UNEQUAL-001', '不等臂天平校准装置(罗马式)', 'STEELYARD', 'UNEQUAL_ARM',
    50.0000, 200.0000, 0.0000,
    0.800000, 500.00, 0.001,
    '黄铜+钢', '基于古罗马Steelyard原理的不等臂校准装置',
    '{"steps":["空载调零","游标定位","标准砝码5点校准","线性度验证"],"tolerance":0.001}'
),
(
    'CAL-PREMIUM-001', '精密玛瑙刀口校准装置', 'PRECISION_ANALYTICAL', 'EQUAL_ARM',
    150.0000, 150.0000, 0.0000,
    0.200000, 200.00, 0.00001,
    '不锈钢+玛瑙', '类似文艺复兴时期精密天平，用于高精度校准',
    '{"steps":["环境温湿度稳定30min","空载预热15min","ABBA循环测量法","11点线性校准","重复性测量10次"],"positions":11,"tolerance":0.00001}'
)
ON CONFLICT (device_code) DO NOTHING;

-- ============================================================
-- 功能四: 虚拟称量体验 - 物品库
-- ============================================================
CREATE TABLE IF NOT EXISTS virtual_weighing_items (
    id SERIAL PRIMARY KEY,
    item_code VARCHAR(50) NOT NULL UNIQUE,
    item_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    era VARCHAR(50),
    civilization VARCHAR(50),
    nominal_mass NUMERIC(10,4),
    actual_mass NUMERIC(10,6),
    volume_cm3 NUMERIC(8,2),
    material VARCHAR(50),
    color VARCHAR(20),
    shape VARCHAR(20),
    icon_url VARCHAR(500),
    historical_significance TEXT,
    rarity VARCHAR(20),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_virtual_items_category ON virtual_weighing_items(category);
CREATE INDEX IF NOT EXISTS idx_virtual_items_civilization ON virtual_weighing_items(civilization);
CREATE INDEX IF NOT EXISTS idx_virtual_items_active ON virtual_weighing_items(is_active);

-- ============================================================
-- 初始化虚拟称量物品库
-- ============================================================
INSERT INTO virtual_weighing_items (
    item_code, item_name, category, era, civilization,
    nominal_mass, actual_mass, volume_cm3, material,
    color, shape, historical_significance, rarity, display_order
) VALUES
-- 砝码类
('WGT-QIN-001', '秦代标准铜权', 'weight', '秦', '中国', 253.0000, 253.0540, 30.0, '青铜', '青绿色', '圆台形', '秦始皇统一度量衡的标准1斤权', 'rare', 1),
('WGT-HAN-001', '西汉金饼', 'weight', '西汉', '中国', 248.0000, 247.9860, 15.0, '金', '金黄色', '圆饼形', '西汉诸侯黄金贡赋', 'legendary', 2),
('WGT-TANG-001', '唐代银铤', 'weight', '唐', '中国', 41.3750, 41.3580, 5.0, '银', '银白色', '长条形', '唐代1两标准银铤', 'rare', 3),
('WGT-MING-001', '万历铜权', 'weight', '明', '中国', 585.0000, 585.1200, 70.0, '青铜', '深绿色', '六面体形', '明代万历年间1斤标准权', 'rare', 4),
('WGT-ROME-001', '罗马标准铜权', 'weight', '古罗马', '罗马', 327.4000, 327.3850, 40.0, '青铜', '青绿色', '半球形', '罗马帝国1磅标准权', 'rare', 5),
('WGT-EGYPT-001', '埃及石权', 'weight', '古埃及', '埃及', 135.0000, 134.9800, 50.0, '石灰岩', '米黄色', '方锥形', '古埃及祭祀用标准权', 'legendary', 6),
('WGT-STD-10G', '标准砝码10g', 'weight', '现代', '国际', 10.0000, 10.00000, 2.0, '不锈钢', '银白色', '圆柱形', '现代E2级标准砝码', 'common', 7),
('WGT-STD-50G', '标准砝码50g', 'weight', '现代', '国际', 50.0000, 50.00000, 8.0, '不锈钢', '银白色', '圆柱形', '现代E2级标准砝码', 'common', 8),
('WGT-STD-100G', '标准砝码100g', 'weight', '现代', '国际', 100.0000, 100.00000, 15.0, '不锈钢', '银白色', '圆柱形', '现代E2级标准砝码', 'common', 9),
('WGT-STD-500G', '标准砝码500g', 'weight', '现代', '国际', 500.0000, 500.00000, 70.0, '不锈钢', '银白色', '圆柱形', '现代E2级标准砝码', 'common', 10),

-- 文物类
('OBJ-BRONZE-DING', '商代青铜小鼎', 'artifact', '商', '中国', 850.0000, 849.7650, 200.0, '青铜', '青绿色', '鼎形', '商代晚期青铜礼器，重约1.7斤', 'legendary', 20),
('OBJ-JADE-BI', '战国玉璧', 'artifact', '战国', '中国', 156.2500, 156.3200, 80.0, '和田玉', '青白色', '圆形', '战国时期1两标准玉璧', 'rare', 21),
('OBJ-GOLD-SEAL', '汉代金印', 'artifact', '西汉', '中国', 108.5000, 108.4920, 10.0, '金', '赤金色', '方形', '西汉诸侯王金印', 'legendary', 22),
('OBJ-SILK-ROLL', '唐代丝绸卷', 'artifact', '唐', '中国', 78.1250, 78.1500, 500.0, '丝绸', '朱红色', '卷形', '唐代5两上等丝绸', 'common', 23),
('OBJ-TEA-CAKE', '宋代茶饼', 'artifact', '宋', '中国', 312.5000, 312.4800, 250.0, '茶', '深褐色', '饼形', '宋代大龙团茶饼，重约20两', 'common', 24),
('OBJ-PORCELAIN', '明代青花瓷碗', 'artifact', '明', '中国', 234.3750, 234.4000, 400.0, '瓷器', '白底青花', '碗形', '明代宣德年间青花碗', 'rare', 25),
('OBJ-ROMAN-COIN', '罗马金币', 'artifact', '古罗马', '罗马', 7.2000, 7.1950, 0.5, '金', '金黄色', '圆形', '罗马奥勒留金币', 'rare', 26),
('OBJ-EGYPT-AMULET', '埃及圣甲虫护符', 'artifact', '古埃及', '埃及', 12.5000, 12.4800, 4.0, '绿松石', '蓝绿色', '甲虫形', '古埃及护身符，约0.8两', 'rare', 27),
('OBJ-GREEK-VASE', '希腊陶瓶', 'artifact', '古希腊', '希腊', 450.0000, 450.1200, 600.0, '陶器', '红黑彩绘', '瓶形', '古希腊黑绘陶瓶', 'rare', 28),
('OBJ-ISLAMIC-SILVER', '伊斯兰银币', 'artifact', '伊斯兰', '中东', 2.9000, 2.8950, 0.3, '银', '银白色', '圆形', '阿拔斯王朝迪尔汗银币', 'common', 29),

-- 日常物品
('ITEM-RICE', '普通大米1升', 'daily', '现代', '通用', 750.0000, 749.8000, 1000.0, '谷物', '白色', '颗粒状', '现代大米1升约1.5斤', 'common', 40),
('ITEM-EGG', '普通鸡蛋', 'daily', '现代', '通用', 50.0000, 49.8000, 50.0, '禽蛋', '米黄色', '椭球形', '一个普通鸡蛋', 'common', 41),
('ITEM-COIN-1YUAN', '1元硬币', 'daily', '现代', '中国', 6.1000, 6.1000, 1.0, '钢芯镀镍', '银白色', '圆形', '中国第五套人民币1元硬币', 'common', 42),
('ITEM-PHONE', '智能手机', 'daily', '现代', '通用', 200.0000, 198.5000, 80.0, '电子', '黑色', '长方形', '普通智能手机约4两', 'common', 43),
('ITEM-KEY', '普通钥匙', 'daily', '现代', '通用', 15.0000, 14.8000, 3.0, '铜', '铜黄色', '钥匙形', '一把普通家门钥匙', 'common', 44),
('ITEM-PEN', '中性笔', 'daily', '现代', '通用', 10.0000, 9.8000, 5.0, '塑料', '蓝色', '圆柱形', '一支普通中性笔', 'common', 45),

-- 趣味物品
('FUN-FEATHER', '鸿毛', 'fun', '通用', '通用', 0.0010, 0.0010, 10.0, '角蛋白', '白色', '羽毛状', '轻如鸿毛，约0.00002两', 'fun', 60),
('FUN-IRON', '千钧铁', 'fun', '通用', '通用', 15000.0000, 15000.0000, 2000.0, '铁', '灰黑色', '长方体', '重若千钧，约30000两', 'fun', 61),
('FUN-GOLD-BAR', '金条1kg', 'fun', '现代', '国际', 1000.0000, 1000.00000, 52.0, '金', '赤金色', '长方体', '标准1公斤金条', 'fun', 62),
('FUN-DIAMOND', '1克拉钻石', 'fun', '现代', '国际', 0.2000, 0.20000, 0.06, '金刚石', '无色透明', '八面体', '1克拉=0.2克，约0.004两', 'fun', 63)
ON CONFLICT (item_code) DO NOTHING;

-- ============================================================
-- 视图: 文明对比雷达图数据视图
-- ============================================================
CREATE OR REPLACE VIEW v_civilization_radar_data AS
SELECT 
    id,
    civilization_name,
    civilization_code,
    period_start_year,
    period_end_year,
    max_capacity,
    relative_precision,
    material_hardness,
    arm_ratio_consistency,
    structure_complexity,
    durability_score,
    radar_data
FROM civilization_balances
ORDER BY period_start_year;

-- ============================================================
-- 函数: 制造工艺评分计算
-- ============================================================
CREATE OR REPLACE FUNCTION calculate_manufacturing_score(
    p_knife_radius NUMERIC,
    p_arm_left NUMERIC,
    p_arm_right NUMERIC,
    p_material VARCHAR,
    p_wear_depth NUMERIC,
    p_friction NUMERIC
) RETURNS JSONB AS $$
DECLARE
    v_geometry_score NUMERIC;
    v_surface_score NUMERIC;
    v_material_score NUMERIC;
    v_assembly_score NUMERIC;
    v_arm_ratio NUMERIC;
    v_grade VARCHAR;
    v_result JSONB;
BEGIN
    -- 几何精度评分: 刀口半径越小(越锋利)分越高
    IF p_knife_radius < 0.5 THEN
        v_geometry_score := 95.0;
    ELSIF p_knife_radius < 1.0 THEN
        v_geometry_score := 85.0;
    ELSIF p_knife_radius < 1.5 THEN
        v_geometry_score := 75.0;
    ELSIF p_knife_radius < 2.0 THEN
        v_geometry_score := 65.0;
    ELSE
        v_geometry_score := 50.0;
    END IF;

    -- 表面粗糙度评分: 从摩擦系数反推，摩擦系数越低表面越光滑
    IF p_friction < 0.0006 THEN
        v_surface_score := 95.0;
    ELSIF p_friction < 0.0010 THEN
        v_surface_score := 85.0;
    ELSIF p_friction < 0.0015 THEN
        v_surface_score := 75.0;
    ELSIF p_friction < 0.0020 THEN
        v_surface_score := 65.0;
    ELSE
        v_surface_score := 50.0;
    END IF;

    -- 材料质量评分: 基于材质
    v_material_score := CASE p_material
        WHEN '钢' THEN 95.0
        WHEN '青铜' THEN 80.0
        WHEN '铁' THEN 75.0
        WHEN '玉石' THEN 90.0
        WHEN '玛瑙' THEN 95.0
        WHEN '木' THEN 55.0
        ELSE 60.0
    END;

    -- 装配精度评分: 臂长比偏差
    IF p_arm_left > 0 AND p_arm_right > 0 THEN
        v_arm_ratio := ABS(p_arm_left - p_arm_right) / ((p_arm_left + p_arm_right) / 2.0);
        IF v_arm_ratio < 0.0001 THEN
            v_assembly_score := 98.0;
        ELSIF v_arm_ratio < 0.0005 THEN
            v_assembly_score := 92.0;
        ELSIF v_arm_ratio < 0.001 THEN
            v_assembly_score := 85.0;
        ELSIF v_arm_ratio < 0.005 THEN
            v_assembly_score := 70.0;
        ELSE
            v_assembly_score := 55.0;
        END IF;
    ELSE
        v_assembly_score := 75.0;
    END IF;

    -- 整体工艺等级
    IF (v_geometry_score + v_surface_score + v_material_score + v_assembly_score) / 4.0 >= 90 THEN
        v_grade := '神品';
    ELSIF (v_geometry_score + v_surface_score + v_material_score + v_assembly_score) / 4.0 >= 80 THEN
        v_grade := '妙品';
    ELSIF (v_geometry_score + v_surface_score + v_material_score + v_assembly_score) / 4.0 >= 70 THEN
        v_grade := '能品';
    ELSIF (v_geometry_score + v_surface_score + v_material_score + v_assembly_score) / 4.0 >= 60 THEN
        v_grade := '佳品';
    ELSE
        v_grade := '常品';
    END IF;

    v_result := jsonb_build_object(
        'geometryScore', v_geometry_score,
        'surfaceScore', v_surface_score,
        'materialScore', v_material_score,
        'assemblyScore', v_assembly_score,
        'overallGrade', v_grade,
        'armRatioError', v_arm_ratio,
        'geometryTolerance', p_knife_radius * 1000,
        'inferredRoughnessRa', p_friction * 100
    );

    RETURN v_result;
END;
$$ LANGUAGE plpgsql;
