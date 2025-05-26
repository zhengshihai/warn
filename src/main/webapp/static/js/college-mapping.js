/**
 * 学院名称与代码的映射关系
 */
const CollegeMapping = {
    // 学院代码到名称的映射
    codeToName: {
        'CS': '计算机学院',
        'ME': '机械工程学院',
        'EE': '电气工程学院',
        'CE': '土木工程学院',
        'CHE': '化学工程学院',
        'ECO': '经济学院',
        'BUS': '商学院',
        'LAW': '法学院',
        'MED': '医学院',
        'ART': '艺术学院',
        'FL': '外国语学院',
        'MATH': '数学学院',
        'PHY': '物理学院',
        'LS': '生命科学学院',
        'ES': '环境学院',
        'IE': '信息工程学院',
        'MS': '材料科学与工程学院',
        'AG': '农学院',
        'ARCH': '建筑学院',
        'PHARM': '药学院'
    },

    // 学院名称到代码的映射
    nameToCode: {
        '计算机学院': 'CS',
        '机械工程学院': 'ME',
        '电气工程学院': 'EE',
        '土木工程学院': 'CE',
        '化学工程学院': 'CHE',
        '经济学院': 'ECO',
        '商学院': 'BUS',
        '法学院': 'LAW',
        '医学院': 'MED',
        '艺术学院': 'ART',
        '外国语学院': 'FL',
        '数学学院': 'MATH',
        '物理学院': 'PHY',
        '生命科学学院': 'LS',
        '环境学院': 'ES',
        '信息工程学院': 'IE',
        '材料科学与工程学院': 'MS',
        '农学院': 'AG',
        '建筑学院': 'ARCH',
        '药学院': 'PHARM'
    },

    /**
     * 根据学院名称获取学院代码
     * @param {string} name 学院名称
     * @returns {string} 学院代码，如果未找到返回 'OTHER'
     */
    getCodeByName: function(name) {
        return this.nameToCode[name] || 'OTHER';
    },

    /**
     * 根据学院代码获取学院名称
     * @param {string} code 学院代码
     * @returns {string} 学院名称，如果未找到返回 null
     */
    getNameByCode: function(code) {
        return this.codeToName[code] || null;
    }
}; 