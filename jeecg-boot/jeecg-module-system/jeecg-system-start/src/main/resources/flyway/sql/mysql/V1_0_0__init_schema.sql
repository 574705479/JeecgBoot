/*
 Navicat Premium Dump SQL

 Source Server         : 127.0.0.1
 Source Server Type    : MySQL
 Source Server Version : 80019 (8.0.19)
 Source Host           : 127.0.0.1:13306
 Source Schema         : jeecg-boot

 Target Server Type    : MySQL
 Target Server Version : 80019 (8.0.19)
 File Encoding         : 65001

 Date: 28/02/2026 15:03:57
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for airag_app
-- ----------------------------
DROP TABLE IF EXISTS `airag_app`;
CREATE TABLE `airag_app`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '租户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用名称',
  `descr` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用描述',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用图标',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用类型',
  `prologue` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '开场白',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提示词',
  `model_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型id',
  `knowledge_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '知识库',
  `flow_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '流程',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态（enable=启用、disable=禁用、release=发布）',
  `msg_num` int NULL DEFAULT NULL COMMENT '历史消息数',
  `metadata` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '元数据',
  `preset_question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '预设问题',
  `quick_command` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '快捷指令',
  `plugins` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '插件',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of airag_app
-- ----------------------------
INSERT INTO `airag_app` VALUES ('2014260441527402498', 'admin', '2026-01-22 16:54:47', 'admin', '2026-02-28 11:30:48', NULL, NULL, '在线客服系统', NULL, '', 'chatSimple', '嘿，亲！我对电影那可是门儿清，能给你带来超棒的电影体验。\n\n![markdown/image_1769072103572.png](http://localhost:8080/jeecg-boot/sys/common/static/markdown/image_1769072103572.png)', '', '2014260361990815746', '2008478334721929217', NULL, 'enable', 1, '{\"modelInfo\":{\"provider\":\"QWEN\",\"modelType\":\"LLM\",\"modelName\":\"qwen3-omni-flash-2025-12-01\"}}', '[{\"key\":1,\"descr\":\"有啥好看的动作片推荐不？\"},{\"key\":2,\"descr\":\"介绍下《流浪地球 3》呗。\"},{\"key\":3,\"descr\":\"啥是电影蒙太奇呀？\"}]', NULL, NULL);

-- ----------------------------
-- Table structure for airag_flow
-- ----------------------------
DROP TABLE IF EXISTS `airag_flow`;
CREATE TABLE `airag_flow`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '租户id',
  `application_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用名称',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
  `descr` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '应用图标',
  `chain` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '编排规则',
  `design` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '编排设计',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态（enable=启用、disable=禁用、release=发布）',
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '元数据',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of airag_flow
-- ----------------------------
INSERT INTO `airag_flow` VALUES ('1892185624983658497', 'admin', '2025-02-19 20:13:03', 'jeecg', '2025-04-24 12:25:08', 'A04', NULL, 'jeecg', '示例_条件分支', NULL, NULL, 'THEN(\n    start.tag(\'start-node\'),\n    SWITCH(switch.tag(\'a448577f-9824-415b-97f6-72543fcb619d\')).to(\n        end.tag(\'91a7df56-107c-4f83-b1e4-b1b7e392c4e3\'),\n        end.tag(\'162160595291774976\')\n    ).tag(\'a448577f-9824-415b-97f6-72543fcb619d\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":515,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"a448577f-9824-415b-97f6-72543fcb619d\",\"type\":\"switch\",\"x\":731,\"y\":486,\"properties\":{\"text\":\"条件分支\",\"options\":{\"if\":[{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"question\",\"operator\":\"CONTAINS\",\"value\":\"jeecg\"}],\"next\":\"162160595291774976\"}],\"else\":{\"next\":\"91a7df56-107c-4f83-b1e4-b1b7e392c4e3\"}},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分支索引\",\"type\":\"number\"}],\"width\":332,\"height\":118}},{\"id\":\"91a7df56-107c-4f83-b1e4-b1b7e392c4e3\",\"type\":\"end\",\"x\":1085,\"y\":662,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{res}}不包含jeecg\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"question\",\"name\":\"res\",\"nodeId\":\"start-node\"}],\"height\":136,\"width\":332}},{\"id\":\"162160595291774976\",\"type\":\"end\",\"x\":1084,\"y\":361,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{res}}包含jeecg\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"question\",\"name\":\"res\",\"nodeId\":\"start-node\"}],\"height\":136,\"width\":332}}],\"edges\":[{\"id\":\"d5124609-d92e-4966-aff8-e220d0d1dbcd\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"a448577f-9824-415b-97f6-72543fcb619d\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"a448577f-9824-415b-97f6-72543fcb619d_input\",\"pointsList\":[{\"x\":466,\"y\":500},{\"x\":566,\"y\":500},{\"x\":465,\"y\":458},{\"x\":565,\"y\":458}]},{\"id\":\"ea3d924a-e4fd-4bb4-bc8a-d1f07119a7eb\",\"type\":\"base-edge\",\"sourceNodeId\":\"a448577f-9824-415b-97f6-72543fcb619d\",\"targetNodeId\":\"91a7df56-107c-4f83-b1e4-b1b7e392c4e3\",\"sourceAnchorId\":\"a448577f-9824-415b-97f6-72543fcb619d_source_else\",\"targetAnchorId\":\"91a7df56-107c-4f83-b1e4-b1b7e392c4e3_input\",\"pointsList\":[{\"x\":897,\"y\":518},{\"x\":997,\"y\":518},{\"x\":819,\"y\":625},{\"x\":919,\"y\":625}]},{\"id\":\"162161801783320576\",\"type\":\"base-edge\",\"sourceNodeId\":\"a448577f-9824-415b-97f6-72543fcb619d\",\"targetNodeId\":\"162160595291774976\",\"sourceAnchorId\":\"a448577f-9824-415b-97f6-72543fcb619d_source_if\",\"targetAnchorId\":\"162160595291774976_input\",\"pointsList\":[{\"x\":897,\"y\":492},{\"x\":997,\"y\":492},{\"x\":818,\"y\":324},{\"x\":918,\"y\":324}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"question\",\"name\":\"用户问题\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":true,\"type\":\"string[]\"},{\"field\":\"content\",\"name\":\"用户问题\",\"required\":true,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1892774140436287490', 'jeecg', '2025-02-21 11:11:36', 'jeecg', '2025-04-24 12:27:02', 'A04', NULL, 'jeecg', '示例_LLM', '', NULL, 'THEN(\n    start.tag(\'start-node\'),\n    llm.tag(\'e9f3470a-f129-4baf-880a-294d7b3bff93\'),\n    end.tag(\'9eb6f5c7-94a6-421f-aa39-7cfd7cec44f1\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":273,\"y\":419,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"e9f3470a-f129-4baf-880a-294d7b3bff93\",\"type\":\"llm\",\"x\":708,\"y\":435,\"properties\":{\"text\":\"llm\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你将扮演一个人物角色李白，以下是关于这个角色的详细设定，请根据这些信息来构建你的回答。 \\n\\n**人物基本信息：**\\n- 你是：李白\\n- 人称：第一人称\\n- 出身背景与上下文：李白出生于安西都护府碎叶城（今吉尔吉斯斯坦托克马克市附近），五岁时随父迁居绵州昌隆县（今四川江油）。他出身于富商家庭，家境优渥，自幼接受良好的教育，遍览诸子百家之书，展现出极高的文学天赋与才情，且喜好剑术，心怀远大抱负，立志在政治与文学上都有所建树，一生渴望入仕报国，却又历经坎坷波折，在仕途上起起落落，最终在诗酒与游历中度过了其传奇的一生。\\n**性格特点：**\\n- 豪放不羁：他不受世俗礼教束缚，行事洒脱，常以狂放之态示人，饮酒作乐，挥毫泼墨，尽显自由奔放的性情。例如 “我本楚狂人，凤歌笑孔丘”，敢于对传统观念表达自己的不羁态度。\\n- 自信豁达：坚信自己的才华与能力，面对困境与挫折时总能以豁达胸怀看待。像 “天生我材必有用，千金散尽还复来”，即便遭遇仕途不顺、生活潦倒，依然对未来充满信心。\\n- 重情重义：珍视友情，与众多友人诗酒唱和，在与友人分别时也会真情流露，如 “桃花潭水深千尺，不及汪伦送我情”，用深情笔触描绘出对友人的不舍与感激。\\n- 浪漫洒脱：充满天马行空的想象，其诗中多有对神仙世界、奇幻自然的描绘，追求精神上的自由与超脱，如 “飞流直下三千尺，疑是银河落九天” 这般充满奇幻瑰丽想象的诗句便是他浪漫性情的写照。\\n**语言风格：**\\n- 富有想象力与夸张手法：常以夸张的笔触描绘事物，营造出强烈的艺术感染力与震撼力，使读者仿佛身临其境。如 “白发三千丈，缘愁似个长”，用极度夸张的白发长度来形容愁绪之深。 \\n- 语言优美且自然流畅：用词精准华丽，却又毫无雕琢之感，诗句如行云流水般自然，读来朗朗上口，兼具音乐性与节奏感。像 “故人西辞黄鹤楼，烟花三月下扬州。孤帆远影碧空尽，唯见长江天际流”，文字优美，意境深远，节奏明快。 \\n- 善用典故与比喻：通过巧妙运用历史典故和形象比喻，增添诗歌的文化底蕴与内涵深度，使诗句更加含蓄蕴藉又易于理解。例如 “闲来垂钓碧溪上，忽复乘舟梦日边”，借用姜太公垂钓与伊尹梦日的典故表达自己对仕途的期待。 \\n**人际关系：**\\n- 与杜甫：李白与杜甫堪称唐代诗坛的双子星，二人相互倾慕，结下深厚情谊。他们曾一同游历，在诗歌创作上相互切磋交流，杜甫有多首诗表达对李白的思念与敬仰，李白也对杜甫颇为欣赏，他们的友情成为文学史上的佳话。\\n- 与汪伦：汪伦以美酒盛情款待李白，李白深受感动，留下 “桃花潭水深千尺，不及汪伦送我情” 的千古名句，可见他们之间真挚的友情。\\n- 与贺知章：贺知章对李白的才华极为赏识，称其为 “谪仙人”，二人在长安官场与诗坛都有交往，这种知遇之情对李白的声誉与心境都产生了积极影响。\\n- 与唐玄宗：李白曾受唐玄宗征召入宫，供奉翰林，本以为可大展政治抱负，然而玄宗只是将他视为文学侍从，为宫廷宴乐作诗助兴，这段君臣关系最终以李白被赐金放还而告终，使李白在仕途理想上遭受重大挫折。\\n**经典台词或口头禅：**\\n- 台词1：“仰天大笑出门去，我辈岂是蓬蒿人。” 表达出其对自身才华的自信以及即将踏入仕途、一展宏图的豪迈与喜悦。 \\n- 台词2：“安能摧眉折腰事权贵，使我不得开心颜。” 体现出他不向权贵低头，坚守人格尊严与精神自由的高尚情操与不屈性格。\\n- 台词2：“长风破浪会有时，直挂云帆济沧海。” 展现出面对困难时的乐观态度与坚定信念，相信总有一天能够乘风破浪，实现理想抱负。\\n\\n要求： \\n- 根据上述提供的角色设定，以第一人称视角进行表达。 \\n- 在回答时，尽可能地融入该角色的性格特点、语言风格以及其特有的口头禅或经典台词。\\n- 如果适用的话，在适当的地方加入（）内的补充信息，如动作、神情等，以增强对话的真实感和生动性。 \"},{\"role\":\"user\",\"content\":\"{{inParam1}}\"}]},\"inputParams\":[{\"nodeId\":\"start-node\",\"name\":\"inParam1\",\"field\":\"content\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"text\"}],\"width\":332,\"height\":180}},{\"id\":\"9eb6f5c7-94a6-421f-aa39-7cfd7cec44f1\",\"type\":\"end\",\"x\":1186,\"y\":467,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"回复：{{回复内容}}\"},\"inputParams\":[],\"outputParams\":[{\"nodeId\":\"e9f3470a-f129-4baf-880a-294d7b3bff93\",\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"text\"}],\"width\":332,\"height\":136}}],\"edges\":[{\"id\":\"ab818150-d4e5-4be2-8d80-31b7f48dc318\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"e9f3470a-f129-4baf-880a-294d7b3bff93\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"e9f3470a-f129-4baf-880a-294d7b3bff93_input\",\"pointsList\":[{\"x\":439,\"y\":404},{\"x\":539,\"y\":404},{\"x\":442,\"y\":376},{\"x\":542,\"y\":376}]},{\"id\":\"158143255481139200\",\"type\":\"base-edge\",\"sourceNodeId\":\"e9f3470a-f129-4baf-880a-294d7b3bff93\",\"targetNodeId\":\"9eb6f5c7-94a6-421f-aa39-7cfd7cec44f1\",\"sourceAnchorId\":\"e9f3470a-f129-4baf-880a-294d7b3bff93_output\",\"targetAnchorId\":\"9eb6f5c7-94a6-421f-aa39-7cfd7cec44f1_input\",\"pointsList\":[{\"x\":874,\"y\":376},{\"x\":974,\"y\":376},{\"x\":920,\"y\":430},{\"x\":1020,\"y\":430}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"},{\"field\":\"content\",\"name\":\"用户问题\",\"required\":true,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1896799016980885506', 'admin', '2025-03-04 13:45:01', 'jeecg', '2025-04-24 12:26:54', 'A04', '', 'jeecg', '示例_分类器', NULL, NULL, 'THEN(\n    start.tag(\'start-node\'),\n    SWITCH(classifier.tag(\'159899349256073216\')).to(\n        end.tag(\'159899421356158976\'),\n        end.tag(\'159899641326432256\'),\n        end.tag(\'159900616165302272\'),\n        end.tag(\'160202618435485696\')\n    ).tag(\'159899349256073216\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":334,\"y\":653,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"内容\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"159899349256073216\",\"type\":\"classifier\",\"x\":714,\"y\":719,\"properties\":{\"text\":\"分类器\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"gpt-4o-mini\",\"temperature\":0.7}},\"categories\":[{\"category\":\"用户问的问题是关于编程的\",\"next\":\"159899421356158976\"},{\"category\":\"用户问的问题是关于食谱的\",\"next\":\"159899641326432256\"},{\"category\":\"其他问题\",\"next\":\"159900616165302272\"}],\"else\":{\"next\":\"160202618435485696\"}},\"inputParams\":[{\"field\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"index\",\"name\":\"分类索引\",\"type\":\"number\"},{\"field\":\"content\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":224,\"width\":332}},{\"id\":\"159899421356158976\",\"type\":\"end\",\"x\":1144,\"y\":566,\"properties\":{\"text\":\"结束1\",\"options\":{\"outputText\":true,\"outputContent\":\"分类：{{分类索引}}\\n-------\\n{{回复内容}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分类索引\",\"nodeId\":\"159899349256073216\"},{\"field\":\"content\",\"name\":\"回复内容\",\"nodeId\":\"159899349256073216\"}],\"height\":136,\"width\":332}},{\"id\":\"159899641326432256\",\"type\":\"end\",\"x\":1144,\"y\":715,\"properties\":{\"text\":\"结束2\",\"options\":{\"outputText\":true,\"outputContent\":\"分类：{{分类索引}}\\n-------\\n{{回复内容}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分类索引\",\"nodeId\":\"159899349256073216\"},{\"field\":\"content\",\"name\":\"回复内容\",\"nodeId\":\"159899349256073216\"}],\"height\":136,\"width\":332}},{\"id\":\"159900616165302272\",\"type\":\"end\",\"x\":1144,\"y\":864,\"properties\":{\"text\":\"结束3\",\"options\":{\"outputText\":true,\"outputContent\":\"分类：{{分类索引}}\\n-------\\n{{回复内容}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分类索引\",\"nodeId\":\"159899349256073216\"},{\"field\":\"content\",\"name\":\"回复内容\",\"nodeId\":\"159899349256073216\"}],\"height\":136,\"width\":332}},{\"id\":\"160202618435485696\",\"type\":\"end\",\"x\":1146,\"y\":1001,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"content\",\"name\":\"res\",\"nodeId\":\"159899349256073216\"}],\"height\":114,\"width\":332}}],\"edges\":[{\"id\":\"159899349260267520\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"159899349256073216\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"159899349256073216_input\",\"pointsList\":[{\"x\":500,\"y\":638},{\"x\":600,\"y\":638},{\"x\":448,\"y\":638},{\"x\":548,\"y\":638}]},{\"id\":\"159899421356158977\",\"type\":\"base-edge\",\"sourceNodeId\":\"159899349256073216\",\"targetNodeId\":\"159899421356158976\",\"sourceAnchorId\":\"159899349256073216_case_1\",\"targetAnchorId\":\"159899421356158976_input\",\"pointsList\":[{\"x\":880,\"y\":672},{\"x\":980,\"y\":672},{\"x\":878,\"y\":529},{\"x\":978,\"y\":529}]},{\"id\":\"159899706925346816\",\"type\":\"base-edge\",\"sourceNodeId\":\"159899349256073216\",\"targetNodeId\":\"159899641326432256\",\"sourceAnchorId\":\"159899349256073216_case_2\",\"targetAnchorId\":\"159899641326432256_input\",\"pointsList\":[{\"x\":880,\"y\":716},{\"x\":980,\"y\":716},{\"x\":878,\"y\":678},{\"x\":978,\"y\":678}]},{\"id\":\"159900640542597120\",\"type\":\"base-edge\",\"sourceNodeId\":\"159899349256073216\",\"targetNodeId\":\"159900616165302272\",\"sourceAnchorId\":\"159899349256073216_case_3\",\"targetAnchorId\":\"159900616165302272_input\",\"pointsList\":[{\"x\":880,\"y\":760},{\"x\":980,\"y\":760},{\"x\":878,\"y\":827},{\"x\":978,\"y\":827}]},{\"id\":\"177966745116012544\",\"type\":\"base-edge\",\"sourceNodeId\":\"159899349256073216\",\"targetNodeId\":\"160202618435485696\",\"sourceAnchorId\":\"159899349256073216_case_else\",\"targetAnchorId\":\"160202618435485696_input\",\"pointsList\":[{\"x\":880,\"y\":804},{\"x\":980,\"y\":804},{\"x\":880,\"y\":975},{\"x\":980,\"y\":975}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"},{\"field\":\"content\",\"name\":\"res\",\"nodeId\":\"159899349256073216\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"内容\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"}]}');
INSERT INTO `airag_flow` VALUES ('1897212806596395009', 'jeecg', '2025-03-05 17:09:16', 'admin', '2025-11-27 17:39:56', 'A04', NULL, 'jeecg', '示例_Jeecg产品助手流程', NULL, NULL, 'THEN(\n    start.tag(\'start-node\'),\n    SWITCH(switch.tag(\'160312505863614464\')).to(\n        THEN(\n            knowledge.tag(\'160312352087846912\'),\n            llm.tag(\'160312692635971584\'),\n            end.tag(\'160312258504536064\')\n        ).tag(\"160312352087846912\"),\n        end.tag(\'162075194587365376\'),\n        THEN(\n            knowledge.tag(\'257078850004389888\'),\n            llm.tag(\'160311787014434816\'),\n            end.tag(\'160312258504536064\')\n        ).tag(\"257078850004389888\")\n    ).tag(\'160312505863614464\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":32.04347826086956,\"y\":-57.34782608695656,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"160311787014434816\",\"type\":\"llm\",\"x\":1018.1304347826085,\"y\":-392.304347826087,\"properties\":{\"text\":\"JeecgLLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"},{\"field\":\"data\",\"name\":\"doc\",\"nodeId\":\"160311730106118144\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":158,\"width\":332}},{\"id\":\"160312258504536064\",\"type\":\"end\",\"x\":1370.695652173913,\"y\":-273.21739130434787,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{jeecgResult}}{{jmResult}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"jeecgResult\",\"nodeId\":\"160311787014434816\"},{\"field\":\"text\",\"name\":\"jmResult\",\"nodeId\":\"160312692635971584\"}],\"height\":136,\"width\":332}},{\"id\":\"160312352087846912\",\"type\":\"knowledge\",\"x\":619.1739130434784,\"y\":-128.36956521739137,\"properties\":{\"text\":\"积木知识库\",\"options\":{\"knowIds\":[\"1897212906878009346\"],\"topNumber\":5,\"similarity\":0.7},\"inputParams\":[{\"field\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"documents\",\"name\":\"文档列表\",\"type\":\"object[]\"},{\"field\":\"data\",\"name\":\"文档内容\",\"type\":\"string\"}],\"height\":163,\"width\":332,\"remarks\":\"积木报表知识库\"}},{\"id\":\"160312505863614464\",\"type\":\"switch\",\"x\":268.82608695652175,\"y\":-251.95652173913044,\"properties\":{\"text\":\"条件分支\",\"options\":{\"if\":[{\"logic\":\"OR\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"content\",\"operator\":\"CONTAINS\",\"value\":\"jeecg\"},{\"nodeId\":\"start-node\",\"field\":\"content\",\"operator\":\"CONTAINS\",\"value\":\"JeecgBoot\"}],\"next\":\"257078850004389888\"},{\"logic\":\"OR\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"content\",\"operator\":\"CONTAINS\",\"value\":\"jimu\"},{\"nodeId\":\"start-node\",\"field\":\"content\",\"operator\":\"CONTAINS\",\"value\":\"积木\"},{\"nodeId\":\"start-node\",\"field\":\"content\",\"operator\":\"CONTAINS\",\"value\":\"报表\"}],\"next\":\"160312352087846912\"}],\"else\":{\"next\":\"162075194587365376\"}},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分支索引\",\"type\":\"number\"}],\"height\":144,\"width\":332}},{\"id\":\"160312692635971584\",\"type\":\"llm\",\"x\":1013.478260869565,\"y\":-190.78260869565224,\"properties\":{\"text\":\"JmLLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"},{\"field\":\"data\",\"name\":\"doc\",\"nodeId\":\"160312352087846912\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":158,\"width\":332}},{\"id\":\"162075194587365376\",\"type\":\"end\",\"x\":599.8260869565215,\"y\":71.91304347826087,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"我不知道这个问题怎么回答呦。\"},\"inputParams\":[],\"outputParams\":[],\"height\":114,\"width\":332}},{\"id\":\"257078850004389888\",\"type\":\"knowledge\",\"x\":602.8260869565217,\"y\":-441.95652173913044,\"properties\":{\"text\":\"知识库\",\"options\":{\"knowIds\":[\"1897926563148648449\"],\"topNumber\":5,\"similarity\":0.7},\"inputParams\":[{\"field\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"documents\",\"name\":\"文档列表\",\"type\":\"object[]\"},{\"field\":\"data\",\"name\":\"文档内容\",\"type\":\"string\"}],\"height\":136,\"width\":332}}],\"edges\":[{\"id\":\"160312258508730368\",\"type\":\"base-edge\",\"sourceNodeId\":\"160311787014434816\",\"targetNodeId\":\"160312258504536064\",\"sourceAnchorId\":\"160311787014434816_output\",\"targetAnchorId\":\"160312258504536064_input\",\"pointsList\":[{\"x\":1184.1304347826085,\"y\":-440.304347826087},{\"x\":1284.1304347826085,\"y\":-440.304347826087},{\"x\":1104.695652173913,\"y\":-310.21739130434787},{\"x\":1204.695652173913,\"y\":-310.21739130434787}]},{\"id\":\"160312505863614465\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"160312505863614464\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"160312505863614464_input\",\"pointsList\":[{\"x\":198.04347826086956,\"y\":-72.34782608695656},{\"x\":298.04347826086956,\"y\":-72.34782608695656},{\"x\":2.826086956521749,\"y\":-292.95652173913044},{\"x\":102.82608695652175,\"y\":-292.95652173913044}]},{\"id\":\"160312567750569984\",\"type\":\"base-edge\",\"sourceNodeId\":\"160312505863614464\",\"targetNodeId\":\"160312352087846912\",\"sourceAnchorId\":\"160312505863614464_case_2\",\"targetAnchorId\":\"160312352087846912_input\",\"pointsList\":[{\"x\":434.82608695652175,\"y\":-232.95652173913044},{\"x\":534.8260869565217,\"y\":-232.95652173913044},{\"x\":353.17391304347836,\"y\":-178.86956521739137},{\"x\":453.17391304347836,\"y\":-178.86956521739137}]},{\"id\":\"160312692635971585\",\"type\":\"base-edge\",\"sourceNodeId\":\"160312352087846912\",\"targetNodeId\":\"160312692635971584\",\"sourceAnchorId\":\"160312352087846912_output\",\"targetAnchorId\":\"160312692635971584_input\",\"pointsList\":[{\"x\":785.1739130434784,\"y\":-178.86956521739137},{\"x\":885.1739130434784,\"y\":-178.86956521739137},{\"x\":747.478260869565,\"y\":-238.78260869565224},{\"x\":847.478260869565,\"y\":-238.78260869565224}]},{\"id\":\"160312712797990912\",\"type\":\"base-edge\",\"sourceNodeId\":\"160312692635971584\",\"targetNodeId\":\"160312258504536064\",\"sourceAnchorId\":\"160312692635971584_output\",\"targetAnchorId\":\"160312258504536064_input\",\"pointsList\":[{\"x\":1179.478260869565,\"y\":-238.78260869565224},{\"x\":1279.478260869565,\"y\":-238.78260869565224},{\"x\":1104.695652173913,\"y\":-310.21739130434787},{\"x\":1204.695652173913,\"y\":-310.21739130434787}]},{\"id\":\"162116168161726464\",\"type\":\"base-edge\",\"sourceNodeId\":\"160312505863614464\",\"targetNodeId\":\"162075194587365376\",\"sourceAnchorId\":\"160312505863614464_source_else\",\"targetAnchorId\":\"162075194587365376_input\",\"pointsList\":[{\"x\":434.82608695652175,\"y\":-206.95652173913044},{\"x\":534.8260869565217,\"y\":-206.95652173913044},{\"x\":333.8260869565215,\"y\":45.913043478260875},{\"x\":433.8260869565215,\"y\":45.913043478260875}]},{\"id\":\"257078850008584192\",\"type\":\"base-edge\",\"sourceNodeId\":\"160312505863614464\",\"targetNodeId\":\"257078850004389888\",\"sourceAnchorId\":\"160312505863614464_source_if\",\"targetAnchorId\":\"257078850004389888_input\",\"pointsList\":[{\"x\":434.82608695652175,\"y\":-258.95652173913044},{\"x\":534.8260869565217,\"y\":-258.95652173913044},{\"x\":336.82608695652175,\"y\":-478.95652173913044},{\"x\":436.82608695652175,\"y\":-478.95652173913044}]},{\"id\":\"257078872452304896\",\"type\":\"base-edge\",\"sourceNodeId\":\"257078850004389888\",\"targetNodeId\":\"160311787014434816\",\"sourceAnchorId\":\"257078850004389888_output\",\"targetAnchorId\":\"160311787014434816_input\",\"pointsList\":[{\"x\":768.8260869565217,\"y\":-478.95652173913044},{\"x\":868.8260869565217,\"y\":-478.95652173913044},{\"x\":752.1304347826085,\"y\":-440.304347826087},{\"x\":852.1304347826085,\"y\":-440.304347826087}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"},{\"field\":\"content\",\"name\":\"用户问题\",\"required\":true,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1897482706871164929', 'jeecg', '2025-03-06 11:01:45', 'jeecg', '2025-04-24 12:27:58', 'A04', NULL, 'jeecg', '示例_脚本组件', NULL, NULL, 'THEN(\n    start.tag(\'start-node\'),\n    code_160582647542648832.tag(\'code_160582647542648832\'),\n    end.tag(\'160583273626406912\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":455,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"内容\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"question\",\"name\":\"内容2\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"code_160582647542648832\",\"type\":\"code\",\"x\":786,\"y\":488,\"properties\":{\"text\":\"脚本执行\",\"options\":{\"codeType\":\"javascript\",\"code\":\"function main(params) {\\n  return {\\n    result: params.arg1 + \'_拼接_\' + params.arg2,\\n  }\\n}\"},\"inputParams\":[{\"field\":\"content\",\"name\":\"arg1\",\"nodeId\":\"start-node\"},{\"field\":\"question\",\"name\":\"arg2\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"result\",\"name\":\"返回结果\",\"type\":\"string\",\"required\":false}],\"height\":158,\"width\":332}},{\"id\":\"160583273626406912\",\"type\":\"end\",\"x\":1272,\"y\":466,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"{{res}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"result\",\"name\":\"res\",\"nodeId\":\"code_160582647542648832\"}],\"height\":114,\"width\":332}}],\"edges\":[{\"id\":\"160582647546843136\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"code_160582647542648832\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"code_160582647542648832_input\",\"pointsList\":[{\"x\":466,\"y\":440},{\"x\":566,\"y\":440},{\"x\":520,\"y\":440},{\"x\":620,\"y\":440}]},{\"id\":\"160583273626406913\",\"type\":\"base-edge\",\"sourceNodeId\":\"code_160582647542648832\",\"targetNodeId\":\"160583273626406912\",\"sourceAnchorId\":\"code_160582647542648832_output\",\"targetAnchorId\":\"160583273626406912_input\",\"pointsList\":[{\"x\":952,\"y\":440},{\"x\":1052,\"y\":440},{\"x\":1006,\"y\":440},{\"x\":1106,\"y\":440}]}]}', 'enable', '{\"outputs\":[{\"field\":\"result\",\"name\":\"res\",\"nodeId\":\"code_160582647542648832\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"内容\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"}]}');
INSERT INTO `airag_flow` VALUES ('1897496956167577601', 'jeecg', '2025-03-06 11:58:23', 'jeecg', '2025-05-20 10:16:28', 'A04', NULL, 'jeecg', '示例_java增强', NULL, NULL, 'THEN(\n    start.tag(\'start-node\'),\n    enhanceJava.tag(\'160591592557232128\'),\n    end.tag(\'160595080985034752\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":456,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"question\",\"name\":\"问题1\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"content\",\"name\":\"问题2\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"160591592557232128\",\"type\":\"enhanceJava\",\"x\":786,\"y\":499,\"properties\":{\"text\":\"Java增强\",\"options\":{\"enhance\":{\"type\":\"spring\",\"path\":\"testAiragEnhance\"}},\"inputParams\":[{\"field\":\"question\",\"name\":\"arg1\",\"nodeId\":\"start-node\"},{\"field\":\"question\",\"name\":\"arg2\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"result\",\"name\":\"返回结果\",\"type\":\"string\",\"required\":false}],\"height\":158,\"width\":332}},{\"id\":\"160595080985034752\",\"type\":\"end\",\"x\":1272,\"y\":477,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{res}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"result\",\"name\":\"res\",\"nodeId\":\"160591592557232128\"}],\"height\":136,\"width\":332}}],\"edges\":[{\"id\":\"160591592565620736\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"160591592557232128\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"160591592557232128_input\",\"pointsList\":[{\"x\":466,\"y\":441},{\"x\":566,\"y\":441},{\"x\":520,\"y\":440},{\"x\":620,\"y\":440}]},{\"id\":\"160595080989229056\",\"type\":\"base-edge\",\"sourceNodeId\":\"160591592557232128\",\"targetNodeId\":\"160595080985034752\",\"sourceAnchorId\":\"160591592557232128_output\",\"targetAnchorId\":\"160595080985034752_input\",\"pointsList\":[{\"x\":952,\"y\":440},{\"x\":1052,\"y\":440},{\"x\":1006,\"y\":440},{\"x\":1106,\"y\":440}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"question\",\"name\":\"问题1\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":true,\"type\":\"string[]\"},{\"field\":\"content\",\"name\":\"问题2\",\"required\":true,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1897528240805830658', 'jeecg', '2025-03-06 14:02:42', 'admin', '2025-03-21 17:26:44', 'A04', NULL, 'jeecg', '示例_子流程', NULL, 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/任务流程设计选择_1742437659702.png', 'THEN(\n    start.tag(\'start-node\'),\n    subflow.tag(\'160621029847842816\'),\n    end.tag(\'160628486900924416\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":334,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"内容\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":true}],\"outputParams\":[],\"height\":62,\"width\":332}},{\"id\":\"160621029847842816\",\"type\":\"subflow\",\"x\":784,\"y\":334,\"properties\":{\"text\":\"子流程\",\"options\":{\"subflowId\":\"1897955542184693762\"},\"inputParams\":[{\"name\":\"question\",\"nameText\":\"用户问题\",\"field\":\"\",\"nodeId\":\"\"},{\"name\":\"content\",\"nameText\":\"用户问题\",\"field\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"outputText\",\"name\":\"outputText\",\"type\":\"string\"}],\"height\":62,\"width\":332}},{\"id\":\"160628486900924416\",\"type\":\"end\",\"x\":1272,\"y\":334,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"outputText\",\"name\":\"result\",\"nodeId\":\"160621029847842816\"}],\"height\":62,\"width\":332}}],\"edges\":[{\"id\":\"160621029852037120\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"160621029847842816\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"160621029847842816_input\",\"pointsList\":[{\"x\":466,\"y\":334},{\"x\":566,\"y\":334},{\"x\":518,\"y\":334},{\"x\":618,\"y\":334}]},{\"id\":\"160628486905118720\",\"type\":\"base-edge\",\"sourceNodeId\":\"160621029847842816\",\"targetNodeId\":\"160628486900924416\",\"sourceAnchorId\":\"160621029847842816_output\",\"targetAnchorId\":\"160628486900924416_input\",\"pointsList\":[{\"x\":950,\"y\":334},{\"x\":1050,\"y\":334},{\"x\":1006,\"y\":334},{\"x\":1106,\"y\":334}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"name\":\"result\",\"nodeId\":\"160621029847842816\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"内容\",\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\"}]}');
INSERT INTO `airag_flow` VALUES ('1897552224058400770', 'jeecg', '2025-03-06 15:38:00', 'admin', '2025-06-25 23:35:33', 'A04', NULL, 'jeecg', '示例_全部脚本', '示例：脚本节点', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/1流程设计_1742437645575.png', 'THEN(\n    start.tag(\'start-node\'),\n    llm.tag(\'160650416019521536\'),\n    WHEN(\n        code_160652991133433856.tag(\'code_160652991133433856\'),\n        code_166081977564753920.tag(\'code_166081977564753920\'),\n        code_167835393352683520.tag(\'code_167835393352683520\')\n    ).tag(\"code_160652991133433856\"),\n    end.tag(\'160656278891560960\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":418,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"160650416019521536\",\"type\":\"llm\",\"x\":693,\"y\":462,\"properties\":{\"text\":\"LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":null,\"topP\":0.9,\"presencePenalty\":0.1,\"frequencyPenalty\":0.1}},\"history\":4,\"messages\":[{\"role\":\"system\",\"content\":\"# 角色\\n你是一位严厉的长辈，面对用户的问题，要以一种带着隐隐批评，暗示问题简单、用户还有很多需要学习的态度来回复。通过大模型模拟李白来对话，回答用户提出的各种问题。\\n\\n\\n## 技能\\n### 技能 1: 回答问题\\n1. 当用户提出问题时，先简要评价问题较为简单，然后给出回答。\\n2. 回答完问题后，适当提及用户还需要加强学习、增长见识等内容。\\n\\n\\n## 限制:\\n- 回复内容必须逻辑清晰、语言通顺，符合严厉长辈的角色设定。 \\n\\n\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"code_160652991133433856\",\"type\":\"code\",\"x\":1135,\"y\":179,\"properties\":{\"text\":\"js\",\"options\":{\"codeType\":\"javascript\",\"code\":\"function main(params) {\\n  if(params.llmRes){\\n    let resLength  = params.llmRes.length\\n    params.llmRes = params.llmRes + \'\\\\n字数：\'+resLength\\n  }\\n  return {\\n    result: params.llmRes,\\n  }\\n}\"},\"inputParams\":[{\"field\":\"text\",\"name\":\"llmRes\",\"nodeId\":\"160650416019521536\"}],\"outputParams\":[{\"field\":\"result\",\"name\":\"返回结果\",\"type\":\"string\",\"required\":false}],\"height\":158,\"width\":332}},{\"id\":\"160656278891560960\",\"type\":\"end\",\"x\":1653,\"y\":449,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"js:{{res}}\\ngroovy:{{res1}}\\nkotlin:{{res2}}\\npython:{{res3}}\\naviator:{{res4}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"result\",\"name\":\"res\",\"nodeId\":\"code_160652991133433856\"},{\"field\":\"result\",\"name\":\"res1\",\"nodeId\":\"code_166081977564753920\"},{\"field\":\"result\",\"name\":\"res2\",\"nodeId\":\"code_166090618376253440\"},{\"field\":\"result\",\"name\":\"res3\",\"nodeId\":\"code_167828303175372800\"},{\"field\":\"result\",\"name\":\"res4\",\"nodeId\":\"code_167835393352683520\"}],\"height\":136,\"width\":332}},{\"id\":\"code_166081977564753920\",\"type\":\"code\",\"x\":1140,\"y\":413,\"properties\":{\"text\":\"groovy\",\"options\":{\"codeType\":\"groovy\",\"code\":\"def main(params) {\\n    if (params.llmRes) {\\n        def resLength = params.llmRes.length()\\n        params.llmRes += \\\"\\\\n字数：\\\" + resLength\\n    }\\n    return [result: params.llmRes]\\n}\"},\"inputParams\":[{\"field\":\"text\",\"name\":\"llmRes\",\"nodeId\":\"160650416019521536\"}],\"outputParams\":[{\"field\":\"result\",\"name\":\"返回结果\",\"type\":\"string\",\"required\":false}],\"height\":158,\"width\":332}},{\"id\":\"code_167835393352683520\",\"type\":\"code\",\"x\":1141,\"y\":667,\"properties\":{\"text\":\"aviator\",\"options\":{\"codeType\":\"aviator\",\"code\":\"let llmRes = params.llmRes;\\nlet resLength = length(llmRes);\\nlet  res = llmRes + \\\"\\\\n字数1：\\\" + resLength;\\nlet resp = seq.map(\\\"result\\\",res);\"},\"inputParams\":[{\"field\":\"text\",\"name\":\"llmRes\",\"nodeId\":\"160650416019521536\"}],\"outputParams\":[{\"field\":\"result\",\"name\":\"返回结果\",\"type\":\"string\"}],\"height\":158,\"width\":332}}],\"edges\":[{\"id\":\"160650416019521537\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"160650416019521536\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"160650416019521536_input\",\"pointsList\":[{\"x\":466,\"y\":403},{\"x\":566,\"y\":403},{\"x\":427,\"y\":403},{\"x\":527,\"y\":403}]},{\"id\":\"160652991137628160\",\"type\":\"base-edge\",\"sourceNodeId\":\"160650416019521536\",\"targetNodeId\":\"code_160652991133433856\",\"sourceAnchorId\":\"160650416019521536_output\",\"targetAnchorId\":\"code_160652991133433856_input\",\"pointsList\":[{\"x\":859,\"y\":403},{\"x\":959,\"y\":403},{\"x\":869,\"y\":131},{\"x\":969,\"y\":131}]},{\"id\":\"160656278899949568\",\"type\":\"base-edge\",\"sourceNodeId\":\"code_160652991133433856\",\"targetNodeId\":\"160656278891560960\",\"sourceAnchorId\":\"code_160652991133433856_output\",\"targetAnchorId\":\"160656278891560960_input\",\"pointsList\":[{\"x\":1301,\"y\":131},{\"x\":1401,\"y\":131},{\"x\":1387,\"y\":412},{\"x\":1487,\"y\":412}]},{\"id\":\"166082001409372160\",\"type\":\"base-edge\",\"sourceNodeId\":\"160650416019521536\",\"targetNodeId\":\"code_166081977564753920\",\"sourceAnchorId\":\"160650416019521536_output\",\"targetAnchorId\":\"code_166081977564753920_input\",\"pointsList\":[{\"x\":859,\"y\":403},{\"x\":959,\"y\":403},{\"x\":874,\"y\":365},{\"x\":974,\"y\":365}]},{\"id\":\"166082017557442560\",\"type\":\"base-edge\",\"sourceNodeId\":\"code_166081977564753920\",\"targetNodeId\":\"160656278891560960\",\"sourceAnchorId\":\"code_166081977564753920_output\",\"targetAnchorId\":\"160656278891560960_input\",\"pointsList\":[{\"x\":1306,\"y\":365},{\"x\":1406,\"y\":365},{\"x\":1387,\"y\":412},{\"x\":1487,\"y\":412}]},{\"id\":\"167835393356877824\",\"type\":\"base-edge\",\"sourceNodeId\":\"160650416019521536\",\"targetNodeId\":\"code_167835393352683520\",\"sourceAnchorId\":\"160650416019521536_output\",\"targetAnchorId\":\"code_167835393352683520_input\",\"pointsList\":[{\"x\":859,\"y\":403},{\"x\":959,\"y\":403},{\"x\":875,\"y\":619},{\"x\":975,\"y\":619}]},{\"id\":\"167836988980817920\",\"type\":\"base-edge\",\"sourceNodeId\":\"code_167835393352683520\",\"targetNodeId\":\"160656278891560960\",\"sourceAnchorId\":\"code_167835393352683520_output\",\"targetAnchorId\":\"160656278891560960_input\",\"pointsList\":[{\"x\":1307,\"y\":619},{\"x\":1407,\"y\":619},{\"x\":1387,\"y\":412},{\"x\":1487,\"y\":412}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"},{\"field\":\"content\",\"name\":\"用户问题\",\"required\":true,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1900021198960492546', 'jeecg', '2025-03-13 11:08:49', 'admin', '2025-11-27 16:49:13', 'A04', NULL, 'jeecg', '示例_回复节点', '', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/流程设计引擎_1742383594151.png', 'THEN(\n    start.tag(\'start-node\'),\n    llm.tag(\'163122102386216960\'),\n    reply.tag(\'163119312863678464\'),\n    llm.tag(\'163122766768164864\'),\n    end.tag(\'163119405809455104\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":232,\"y\":273,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":true}],\"outputParams\":[],\"height\":62,\"width\":332}},{\"id\":\"163119312863678464\",\"type\":\"reply\",\"x\":800,\"y\":225,\"properties\":{\"text\":\"直接回复\",\"options\":{\"content\":\"{{content}}\"},\"inputParams\":[{\"field\":\"text\",\"name\":\"content\",\"nodeId\":\"163122102386216960\"}],\"outputParams\":[],\"height\":62,\"width\":332}},{\"id\":\"163119405809455104\",\"type\":\"end\",\"x\":1548,\"y\":254,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{resp}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"resp\",\"nodeId\":\"163122766768164864\"}],\"height\":62,\"width\":332}},{\"id\":\"163122102386216960\",\"type\":\"llm\",\"x\":551,\"y\":553,\"properties\":{\"text\":\"LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"根据用户的问题，以有趣的方式回答，如果可以的话请引用故事或经典说明。\\n\\n用中文回复。\\n\\n字数控制在200以内。\"},{\"role\":\"user\",\"content\":\"{{content}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":136,\"width\":332}},{\"id\":\"163122766768164864\",\"type\":\"llm\",\"x\":1144,\"y\":412,\"properties\":{\"text\":\"nextQue\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"根据用户的问题和ai的回复，猜测用户下一次的问题可能有哪些，markdown格式回复。\\n格式：\\n\\\\n你可能还想知道：\\n* 问题一\\n* 问题二\\n。。。。\"},{\"role\":\"user\",\"content\":\"用户问题：{{que}}\\nAI回复：{{res}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"que\",\"nodeId\":\"start-node\"},{\"field\":\"text\",\"name\":\"res\",\"nodeId\":\"163122102386216960\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":136,\"width\":332}}],\"edges\":[{\"id\":\"163122102390411264\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"163122102386216960\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"163122102386216960_input\",\"pointsList\":[{\"x\":398,\"y\":273},{\"x\":498,\"y\":273},{\"x\":285,\"y\":516},{\"x\":385,\"y\":516}]},{\"id\":\"163122147491762176\",\"type\":\"base-edge\",\"sourceNodeId\":\"163122102386216960\",\"targetNodeId\":\"163119312863678464\",\"sourceAnchorId\":\"163122102386216960_output\",\"targetAnchorId\":\"163119312863678464_input\",\"pointsList\":[{\"x\":717,\"y\":516},{\"x\":817,\"y\":516},{\"x\":534,\"y\":225},{\"x\":634,\"y\":225}]},{\"id\":\"163122766772359168\",\"type\":\"base-edge\",\"sourceNodeId\":\"163119312863678464\",\"targetNodeId\":\"163122766768164864\",\"sourceAnchorId\":\"163119312863678464_output\",\"targetAnchorId\":\"163122766768164864_input\",\"pointsList\":[{\"x\":966,\"y\":225},{\"x\":1066,\"y\":225},{\"x\":878,\"y\":375},{\"x\":978,\"y\":375}]},{\"id\":\"163123226145116160\",\"type\":\"base-edge\",\"sourceNodeId\":\"163122766768164864\",\"targetNodeId\":\"163119405809455104\",\"sourceAnchorId\":\"163122766768164864_output\",\"targetAnchorId\":\"163119405809455104_input\",\"pointsList\":[{\"x\":1310,\"y\":375},{\"x\":1410,\"y\":375},{\"x\":1282,\"y\":254},{\"x\":1382,\"y\":254}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\"}]}');
INSERT INTO `airag_flow` VALUES ('1900029596154232833', 'jeecg', '2025-03-13 11:42:11', 'admin', '2025-11-27 16:49:22', 'A04', NULL, 'jeecg', '示例_Http节点', '', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/流程设计(1)_1742383583093.png', 'THEN(\n    start.tag(\'start-node\'),\n    http.tag(\'163206941950185472\'),\n    SWITCH(switch.tag(\'163207852529389568\')).to(\n        THEN(\n            http.tag(\'163128964742746112\'),\n            SWITCH(switch.tag(\'168299837777608704\')).to(\n                end.tag(\'163129833764786176\'),\n                end.tag(\'168300140241453056\')\n            ).tag(\'168299837777608704\')\n        ).tag(\"163128964742746112\"),\n        end.tag(\'163208186282741760\')\n    ).tag(\'163207852529389568\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":51.13043478260868,\"y\":342.804347826087,\"properties\":{\"text\":\"开始\",\"remarks\":\"大萨达撒\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":true}],\"outputParams\":[],\"height\":89,\"width\":332}},{\"id\":\"163128964742746112\",\"type\":\"http\",\"x\":859.0869565217391,\"y\":192.2173913043478,\"properties\":{\"text\":\"HTTP 请求 查询\",\"options\":{\"http\":{\"url\":\"{{domainURL}}/test/jeecgDemo/list\",\"method\":\"GET\",\"headers\":{},\"requestBody\":{\"type\":\"none\",\"body\":\"\"},\"requestParams\":{\"name\":\"{{name}}\",\"pageNo\":\"1\",\"pageSize\":\"10\"},\"timeout\":120}},\"inputParams\":[{\"field\":\"content\",\"name\":\"name\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"body\",\"name\":\"输出\",\"type\":\"string\",\"required\":false},{\"field\":\"statusCode\",\"name\":\"状态码\",\"type\":\"number\"},{\"field\":\"body.success\",\"name\":\"是否成功\",\"type\":\"string\",\"required\":false},{\"field\":\"body.result.records[0].id\",\"name\":\"id\",\"type\":\"string\",\"required\":false}],\"height\":62,\"width\":332}},{\"id\":\"163129833764786176\",\"type\":\"end\",\"x\":1386.5217391304348,\"y\":164.08695652173913,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"新增的用户Id：{{id}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"body.result.records[0].id\",\"name\":\"id\",\"nodeId\":\"163128964742746112\"}],\"height\":62,\"width\":332}},{\"id\":\"163206941950185472\",\"type\":\"http\",\"x\":320.1304347826087,\"y\":474.2173913043478,\"properties\":{\"text\":\"HTTP 请求\",\"options\":{\"http\":{\"url\":\"{{domainURL}}/test/jeecgDemo/add\",\"method\":\"POST\",\"headers\":{},\"requestBody\":{\"type\":\"json\",\"body\":\"{\\n  \\\"name\\\": \\\"{{name}}\\\",\\n  \\\"keyWord\\\": \\\"example\\\",\\n  \\\"punchTime\\\": \\\"2023-10-05 14:48:00\\\",\\n  \\\"salaryMoney\\\": 1000.00,\\n  \\\"bonusMoney\\\": 500.0,\\n  \\\"sex\\\": \\\"1\\\",\\n  \\\"age\\\": 30,\\n  \\\"birthday\\\": \\\"2023-10-05\\\",\\n  \\\"email\\\": \\\"john.doe@example.com\\\",\\n  \\\"content\\\": \\\"This is a test content.\\\",\\n}\"},\"requestParams\":{},\"timeout\":120}},\"inputParams\":[{\"field\":\"content\",\"name\":\"name\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"statusCode\",\"name\":\"code\",\"type\":\"string\",\"required\":false},{\"field\":\"body\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":62,\"width\":332}},{\"id\":\"163207852529389568\",\"type\":\"switch\",\"x\":510.78260869565224,\"y\":302.73913043478257,\"properties\":{\"text\":\"条件分支\",\"options\":{\"if\":[{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"163206941950185472\",\"field\":\"statusCode\",\"operator\":\"EQUALS\",\"value\":\"200\"}],\"next\":\"163128964742746112\"}],\"else\":{\"next\":\"163208186282741760\"}},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分支索引\",\"type\":\"number\"}],\"height\":118,\"width\":332}},{\"id\":\"163208186282741760\",\"type\":\"end\",\"x\":745.7826086956521,\"y\":448.0869565217391,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"添加数据失败\"},\"inputParams\":[],\"outputParams\":[],\"height\":62,\"width\":332}},{\"id\":\"168299837777608704\",\"type\":\"switch\",\"x\":1029.173913043478,\"y\":314.78260869565213,\"properties\":{\"text\":\"条件分支\",\"options\":{\"if\":[{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"163128964742746112\",\"field\":\"body.success\",\"operator\":\"EQUALS\",\"value\":\"true\"}],\"next\":\"163129833764786176\"}],\"else\":{\"next\":\"168300140241453056\"}},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分支索引\",\"type\":\"number\"}],\"height\":118,\"width\":332}},{\"id\":\"168300140241453056\",\"type\":\"end\",\"x\":1389.2608695652173,\"y\":419.8695652173913,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"添加用户失败\"},\"inputParams\":[],\"outputParams\":[],\"height\":62,\"width\":332}}],\"edges\":[{\"id\":\"163206941954379776\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"163206941950185472\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"163206941950185472_input\",\"pointsList\":[{\"x\":217.1304347826091,\"y\":329.304347826087},{\"x\":317.1304347826091,\"y\":329.304347826087},{\"x\":54.13043478260869,\"y\":474.2173913043478},{\"x\":154.1304347826087,\"y\":474.2173913043478}]},{\"id\":\"163207852533583872\",\"type\":\"base-edge\",\"sourceNodeId\":\"163206941950185472\",\"targetNodeId\":\"163207852529389568\",\"sourceAnchorId\":\"163206941950185472_output\",\"targetAnchorId\":\"163207852529389568_input\",\"pointsList\":[{\"x\":486.13043478260863,\"y\":474.2173913043478},{\"x\":586.1304347826085,\"y\":474.2173913043478},{\"x\":244.78260869565224,\"y\":274.73913043478257},{\"x\":344.78260869565224,\"y\":274.73913043478257}]},{\"id\":\"163208000881922048\",\"type\":\"base-edge\",\"sourceNodeId\":\"163207852529389568\",\"targetNodeId\":\"163128964742746112\",\"sourceAnchorId\":\"163207852529389568_source_if\",\"targetAnchorId\":\"163128964742746112_input\",\"pointsList\":[{\"x\":676.7826086956521,\"y\":308.73913043478257},{\"x\":776.7826086956521,\"y\":308.73913043478257},{\"x\":593.0869565217391,\"y\":192.2173913043478},{\"x\":693.0869565217391,\"y\":192.2173913043478}]},{\"id\":\"163208186286936064\",\"type\":\"base-edge\",\"sourceNodeId\":\"163207852529389568\",\"targetNodeId\":\"163208186282741760\",\"sourceAnchorId\":\"163207852529389568_source_else\",\"targetAnchorId\":\"163208186282741760_input\",\"pointsList\":[{\"x\":676.7826086956521,\"y\":334.73913043478257},{\"x\":776.7826086956521,\"y\":334.73913043478257},{\"x\":479.78260869565213,\"y\":448.0869565217391},{\"x\":579.7826086956521,\"y\":448.0869565217391}]},{\"id\":\"168299837781803008\",\"type\":\"base-edge\",\"sourceNodeId\":\"163128964742746112\",\"targetNodeId\":\"168299837777608704\",\"sourceAnchorId\":\"163128964742746112_output\",\"targetAnchorId\":\"168299837777608704_input\",\"pointsList\":[{\"x\":1025.086956521739,\"y\":192.2173913043478},{\"x\":1125.0869565217386,\"y\":192.2173913043478},{\"x\":763.173913043478,\"y\":286.78260869565213},{\"x\":863.173913043478,\"y\":286.78260869565213}]},{\"id\":\"168300025623707648\",\"type\":\"base-edge\",\"sourceNodeId\":\"168299837777608704\",\"targetNodeId\":\"163129833764786176\",\"sourceAnchorId\":\"168299837777608704_source_if\",\"targetAnchorId\":\"163129833764786176_input\",\"pointsList\":[{\"x\":1195.1739130434776,\"y\":320.78260869565213},{\"x\":1295.1739130434776,\"y\":320.78260869565213},{\"x\":1120.5217391304348,\"y\":164.08695652173913},{\"x\":1220.5217391304348,\"y\":164.08695652173913}]},{\"id\":\"168300140245647360\",\"type\":\"base-edge\",\"sourceNodeId\":\"168299837777608704\",\"targetNodeId\":\"168300140241453056\",\"sourceAnchorId\":\"168299837777608704_source_else\",\"targetAnchorId\":\"168300140241453056_input\",\"pointsList\":[{\"x\":1195.1739130434776,\"y\":346.78260869565213},{\"x\":1295.1739130434776,\"y\":346.78260869565213},{\"x\":1123.2608695652173,\"y\":419.8695652173913},{\"x\":1223.2608695652173,\"y\":419.8695652173913}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\"}]}');
INSERT INTO `airag_flow` VALUES ('1902263524520935425', 'jeecg', '2025-03-19 15:39:01', 'admin', '2025-11-27 16:52:31', 'A04', NULL, 'jeecg', 'AI看图说话', '', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/工具-图片解析_1743065064801.png', 'THEN(\n    start.tag(\'start-node\'),\n    llm.tag(\'165363942517174272\'),\n    llm.tag(\'168280528419778560\'),\n    end.tag(\'165364368465522688\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":457,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":true},{\"field\":\"images\",\"name\":\"图片\",\"type\":\"picture\",\"required\":false}],\"outputParams\":[],\"height\":62,\"width\":332}},{\"id\":\"165363942517174272\",\"type\":\"llm\",\"x\":675,\"y\":341,\"properties\":{\"text\":\"图片解读\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你是一个图像分析专家，负责解读和解释用户发送的图片。请根据以下要求进行分析：\\n\\n## 目标：\\n分析并解释图片的意义，提供详细的解读和背景信息。\\n\\n## 技能：\\n1. 视觉识别能力：能够识别图像中的元素及其关系。\\n2. 上下文理解能力：结合文化、历史、艺术等背景知识进行深度解读。\\n3. 清晰表达能力：用简洁明了的语言传达分析结果。\\n\\n## 工作流：\\n1. 识别图片中的主要元素，描述它们的外观和特征。\\n2. 分析这些元素之间的关系及其在整体构图中的作用。\\n3. 提供与图片相关的背景信息，探讨其潜在意义和影响。\\n\\n## 输出格式：\\n- 图片元素描述\\n- 元素关系分析\\n- 背景信息与意义解释\\n\\n## 限制：\\n- 不提供主观判断，仅基于客观分析进行解释。\\n- 不涉及任何隐私或敏感内容的讨论。\"},{\"role\":\"user\",\"content\":\"分析并解释图片的意义，提供详细的解读和背景信息。\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"que\",\"nodeId\":\"start-node\"},{\"field\":\"images\",\"name\":\"images\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":136,\"width\":332}},{\"id\":\"165364368465522688\",\"type\":\"end\",\"x\":1520,\"y\":426,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{resp}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"resp\",\"nodeId\":\"168280528419778560\"}],\"height\":62,\"width\":332}},{\"id\":\"168280528419778560\",\"type\":\"llm\",\"x\":1063,\"y\":588,\"properties\":{\"text\":\"LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你将扮演一个故事创作者，以下是关于这个角色的详细设定，请根据这些信息来构建你的回答。\\n\\n**人物基本信息：**\\n- 你是：一个富有想象力和创造力的故事编写者\\n- 人称：第一人称\\n- 出身背景与上下文：擅长根据不同的元素与情境构建引人入胜的故事，灵感来源于观察与分析\\n**性格特点：**\\n- 富有创造力\\n- 敏感细腻\\n- 善于捕捉细节\\n**语言风格：**\\n- 优雅而富有表现力，能够生动描绘场景与人物情感\\n**人际关系：**\\n- 与其他艺术创作者合作，互相激励\\n**过往经历：**\\n- 多次参与文学比赛并获奖，积累了丰富的创作经验\\n**经典台词或口头禅：**\\n- \\\"每一个画面背后都有一个故事在等待被讲述。\\\"\\n- \\\"细节决定成败。\\\"\\n\\n要求： \\n- 故事应围绕从图片分析得出的主题和情感进行展开。\\n- 包含鲜明的人物、情节以及转折。\\n- 语言生动形象，能够引起读者的共鸣。\\n- 直接讲故事，不要提及图片。\"},{\"role\":\"user\",\"content\":\"{{readImg}}\"}]},\"inputParams\":[{\"field\":\"text\",\"name\":\"readImg\",\"nodeId\":\"165363942517174272\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":136,\"width\":332}}],\"edges\":[{\"id\":\"165363942525562880\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"165363942517174272\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"165363942517174272_input\",\"pointsList\":[{\"x\":466,\"y\":457},{\"x\":566,\"y\":457},{\"x\":409,\"y\":304},{\"x\":509,\"y\":304}]},{\"id\":\"168280528428167168\",\"type\":\"base-edge\",\"sourceNodeId\":\"165363942517174272\",\"targetNodeId\":\"168280528419778560\",\"sourceAnchorId\":\"165363942517174272_output\",\"targetAnchorId\":\"168280528419778560_input\",\"pointsList\":[{\"x\":841,\"y\":304},{\"x\":941,\"y\":304},{\"x\":797,\"y\":551},{\"x\":897,\"y\":551}]},{\"id\":\"168280631234752512\",\"type\":\"base-edge\",\"sourceNodeId\":\"168280528419778560\",\"targetNodeId\":\"165364368465522688\",\"sourceAnchorId\":\"168280528419778560_output\",\"targetAnchorId\":\"165364368465522688_input\",\"pointsList\":[{\"x\":1229,\"y\":551},{\"x\":1329,\"y\":551},{\"x\":1254,\"y\":426},{\"x\":1354,\"y\":426}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\"},{\"field\":\"images\",\"name\":\"图片\",\"type\":\"picture\"}]}');
INSERT INTO `airag_flow` VALUES ('1904779811574784002', 'jeecg', '2025-03-26 14:17:51', 'admin', '2025-11-27 16:45:18', 'A04', NULL, 'jeecg', '示例_OCR', '', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/1dataOCR_1743065089791.png', 'THEN(\n    start.tag(\'start-node\'),\n    SWITCH(switch.tag(\'167880707187527680\')).to(\n        end.tag(\'167880856269869056\'),\n        THEN(\n            code_167881149430747136.tag(\'code_167881149430747136\'),\n            llm.tag(\'167881839356006400\'),\n            end.tag(\'167880661561888768\')\n        ).tag(\"code_167881149430747136\")\n    ).tag(\'167880707187527680\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":421,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"images\",\"name\":\"图片\",\"type\":\"picture\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"167880661561888768\",\"type\":\"end\",\"x\":1474,\"y\":342,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"data\",\"nodeId\":\"167881839356006400\"}],\"height\":114,\"width\":332}},{\"id\":\"167880707187527680\",\"type\":\"switch\",\"x\":681,\"y\":233,\"properties\":{\"text\":\"条件分支\",\"options\":{\"if\":[{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"images\",\"operator\":\"EMPTY\",\"value\":\"\"}],\"next\":\"167880856269869056\"}],\"else\":{\"next\":\"code_167881149430747136\"}},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分支索引\",\"type\":\"number\"}],\"height\":118,\"width\":332}},{\"id\":\"167880856269869056\",\"type\":\"end\",\"x\":1207,\"y\":207,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{\\n    \\\"message\\\": \\\"请提供图片\\\"\\n  }\"},\"inputParams\":[],\"outputParams\":[],\"height\":114,\"width\":332}},{\"id\":\"code_167881149430747136\",\"type\":\"code\",\"x\":937,\"y\":460,\"properties\":{\"text\":\"脚本执行\",\"options\":{\"codeType\":\"groovy\",\"code\":\"def main(Map params) {\\n    def newQuestion = params.question\\n    if (!params.question) {\\n        newQuestion = \\\"从图片中提取文字\\\"\\n    }\\n    return [result: newQuestion]\\n}\\n\"},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"result\",\"name\":\"返回结果\",\"type\":\"string\"}],\"height\":158,\"width\":332}},{\"id\":\"167881839356006400\",\"type\":\"llm\",\"x\":1319,\"y\":607,\"properties\":{\"text\":\"LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"# 角色：OCR工具\\n作为一个智能OCR工具，你的主要职责是从图片中提取文字并将其输出为结构化数据。\\n\\n## 目标：\\n1. 精确识别和提取图片中的文字信息。\\n2. 将提取的文字转换为结构化数据格式。\\n\\n## 技能：\\n1. 高效的图像处理能力。\\n2. 精确的文字识别算法。\\n3. 数据格式化与输出能力。\\n\\n## 工作流：\\n1. 输入图片，进行预处理（如去噪、二值化）。\\n2. 应用OCR算法识别图片中的文字，并记录识别结果。\\n3. 将识别的文字整理成结构化数据格式，如JSON或CSV。\\n\\n## 输出格式：\\n提取的文本应以结构化数据格式输出，如：\\n{\\n    \\\"text\\\": \\\"提取的内容\\\",\\n    \\\"metadata\\\": {\\\"source\\\": \\\"图片来源\\\", \\\"timestamp\\\": \\\"提取时间\\\"}\\n  }\\n\\n## 限制：\\n- 仅限于合法和合规的图片内容提取。\\n- 不得保存用户上传的图片数据。\\n- 需确保输出的数据准确无误，标注所有数据来源。\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]},\"inputParams\":[{\"field\":\"images\",\"name\":\"images\",\"nodeId\":\"start-node\"},{\"field\":\"result\",\"name\":\"question\",\"nodeId\":\"code_167881149430747136\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}}],\"edges\":[{\"id\":\"167880707195916288\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"167880707187527680\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"167880707187527680_input\",\"pointsList\":[{\"x\":466,\"y\":406},{\"x\":566,\"y\":406},{\"x\":415,\"y\":205},{\"x\":515,\"y\":205}]},{\"id\":\"167880856274063360\",\"type\":\"base-edge\",\"sourceNodeId\":\"167880707187527680\",\"targetNodeId\":\"167880856269869056\",\"sourceAnchorId\":\"167880707187527680_source_if\",\"targetAnchorId\":\"167880856269869056_input\",\"pointsList\":[{\"x\":847,\"y\":239},{\"x\":947,\"y\":239},{\"x\":941,\"y\":181},{\"x\":1041,\"y\":181}]},{\"id\":\"167881149434941440\",\"type\":\"base-edge\",\"sourceNodeId\":\"167880707187527680\",\"targetNodeId\":\"code_167881149430747136\",\"sourceAnchorId\":\"167880707187527680_source_else\",\"targetAnchorId\":\"code_167881149430747136_input\",\"pointsList\":[{\"x\":847,\"y\":265},{\"x\":947,\"y\":265},{\"x\":671,\"y\":412},{\"x\":771,\"y\":412}]},{\"id\":\"167881839356006401\",\"type\":\"base-edge\",\"sourceNodeId\":\"code_167881149430747136\",\"targetNodeId\":\"167881839356006400\",\"sourceAnchorId\":\"code_167881149430747136_output\",\"targetAnchorId\":\"167881839356006400_input\",\"pointsList\":[{\"x\":1103,\"y\":412},{\"x\":1203,\"y\":412},{\"x\":1053,\"y\":548},{\"x\":1153,\"y\":548}]},{\"id\":\"167882293611712512\",\"type\":\"base-edge\",\"sourceNodeId\":\"167881839356006400\",\"targetNodeId\":\"167880661561888768\",\"sourceAnchorId\":\"167881839356006400_output\",\"targetAnchorId\":\"167880661561888768_input\",\"pointsList\":[{\"x\":1485,\"y\":548},{\"x\":1585,\"y\":548},{\"x\":1208,\"y\":316},{\"x\":1308,\"y\":316}]}]}', 'enable', '{\"outputs\":[{\"field\":\"text\",\"name\":\"data\",\"nodeId\":\"167881839356006400\"},{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":true,\"type\":\"string[]\"},{\"field\":\"images\",\"name\":\"图片\",\"required\":true,\"type\":\"picture\"}]}');
INSERT INTO `airag_flow` VALUES ('1905158829855784962', 'jeecg', '2025-03-27 15:23:56', 'jeecg', '2025-03-27 16:29:22', 'A04', NULL, 'jeecg', '示例_翻译', '', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/翻译_1743060940605.png', 'THEN(\n    start.tag(\'start-node\'),\n    SWITCH(switch.tag(\'168262809717821440\')).to(\n        end.tag(\'168259683329757184\'),\n        THEN(\n            SWITCH(classifier.tag(\'168263048935755776\')).to(\n                llm.tag(\'168263321821368320\'),\n                llm.tag(\'168263346282549248\')\n            ).tag(\'168263048935755776\'),\n            end.tag(\'168263794896916480\')\n        ).tag(\"168263048935755776\")\n    ).tag(\'168262809717821440\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":457,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":true}],\"outputParams\":[],\"height\":62,\"width\":332}},{\"id\":\"168259683329757184\",\"type\":\"end\",\"x\":1090,\"y\":150,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"content\",\"name\":\"data\",\"nodeId\":\"start-node\"}],\"height\":62,\"width\":332}},{\"id\":\"168262809717821440\",\"type\":\"switch\",\"x\":701,\"y\":281,\"properties\":{\"text\":\"条件分支\",\"options\":{\"if\":[{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"content\",\"operator\":\"EMPTY\",\"value\":\"\"}],\"next\":\"168259683329757184\"}],\"else\":{\"next\":\"168263048935755776\"}},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分支索引\",\"type\":\"number\"}],\"height\":118,\"width\":332}},{\"id\":\"168263048935755776\",\"type\":\"classifier\",\"x\":1086,\"y\":381,\"properties\":{\"text\":\"分类器\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.2}},\"categories\":[{\"category\":\"是中文\",\"next\":\"168263321821368320\"}],\"else\":{\"next\":\"168263346282549248\"}},\"inputParams\":[{\"field\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"index\",\"name\":\"分类索引\",\"type\":\"number\"},{\"field\":\"content\",\"name\":\"分类内容\",\"type\":\"string\"}],\"height\":118,\"width\":332}},{\"id\":\"168263321821368320\",\"type\":\"llm\",\"x\":1513,\"y\":292,\"properties\":{\"text\":\"翻译成英文\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.3}},\"history\":1,\"messages\":[{\"role\":\"system\",\"content\":\"将用户输入完整翻译成英文，包括所有语气词和重复表达\\n- 严格保留原始语序和强调成分\\n- 禁止省略任何字词或改变语气强度\\n- 直接输出翻译结果不做解释\"},{\"role\":\"user\",\"content\":\"{{content}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":136,\"width\":332}},{\"id\":\"168263346282549248\",\"type\":\"llm\",\"x\":1514,\"y\":489,\"properties\":{\"text\":\"翻译成中文\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.3}},\"history\":1,\"messages\":[{\"role\":\"system\",\"content\":\"将用户输入完整翻译成中文，包括所有语气词和重复表达\\n- 严格保留原始语序和强调成分\\n- 禁止省略任何字词或改变语气强度\\n- 直接输出翻译结果不做解释\"},{\"role\":\"user\",\"content\":\"{{content}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":136,\"width\":332}},{\"id\":\"168263794896916480\",\"type\":\"end\",\"x\":1982,\"y\":360,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{dataC}}{{dataE}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"dataC\",\"nodeId\":\"168263346282549248\"},{\"field\":\"text\",\"name\":\"dataE\",\"nodeId\":\"168263321821368320\"}],\"height\":62,\"width\":332}}],\"edges\":[{\"id\":\"168262809722015744\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"168262809717821440\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"168262809717821440_input\",\"pointsList\":[{\"x\":466,\"y\":457},{\"x\":566,\"y\":457},{\"x\":435,\"y\":253},{\"x\":535,\"y\":253}]},{\"id\":\"168262871336341504\",\"type\":\"base-edge\",\"sourceNodeId\":\"168262809717821440\",\"targetNodeId\":\"168259683329757184\",\"sourceAnchorId\":\"168262809717821440_source_if\",\"targetAnchorId\":\"168259683329757184_input\",\"pointsList\":[{\"x\":867,\"y\":287},{\"x\":967,\"y\":287},{\"x\":824,\"y\":150},{\"x\":924,\"y\":150}]},{\"id\":\"168263048939950080\",\"type\":\"base-edge\",\"sourceNodeId\":\"168262809717821440\",\"targetNodeId\":\"168263048935755776\",\"sourceAnchorId\":\"168262809717821440_source_else\",\"targetAnchorId\":\"168263048935755776_input\",\"pointsList\":[{\"x\":867,\"y\":313},{\"x\":967,\"y\":313},{\"x\":820,\"y\":353},{\"x\":920,\"y\":353}]},{\"id\":\"168263321825562624\",\"type\":\"base-edge\",\"sourceNodeId\":\"168263048935755776\",\"targetNodeId\":\"168263321821368320\",\"sourceAnchorId\":\"168263048935755776_case_1\",\"targetAnchorId\":\"168263321821368320_input\",\"pointsList\":[{\"x\":1252,\"y\":387},{\"x\":1352,\"y\":387},{\"x\":1247,\"y\":255},{\"x\":1347,\"y\":255}]},{\"id\":\"168263346286743552\",\"type\":\"base-edge\",\"sourceNodeId\":\"168263048935755776\",\"targetNodeId\":\"168263346282549248\",\"sourceAnchorId\":\"168263048935755776_case_else\",\"targetAnchorId\":\"168263346282549248_input\",\"pointsList\":[{\"x\":1252,\"y\":413},{\"x\":1352,\"y\":413},{\"x\":1248,\"y\":452},{\"x\":1348,\"y\":452}]},{\"id\":\"168263794901110784\",\"type\":\"base-edge\",\"sourceNodeId\":\"168263346282549248\",\"targetNodeId\":\"168263794896916480\",\"sourceAnchorId\":\"168263346282549248_output\",\"targetAnchorId\":\"168263794896916480_input\",\"pointsList\":[{\"x\":1680,\"y\":452},{\"x\":1780,\"y\":452},{\"x\":1716,\"y\":360},{\"x\":1816,\"y\":360}]},{\"id\":\"168263831215394816\",\"type\":\"base-edge\",\"sourceNodeId\":\"168263321821368320\",\"targetNodeId\":\"168263794896916480\",\"sourceAnchorId\":\"168263321821368320_output\",\"targetAnchorId\":\"168263794896916480_input\",\"pointsList\":[{\"x\":1679,\"y\":255},{\"x\":1779,\"y\":255},{\"x\":1716,\"y\":360},{\"x\":1816,\"y\":360}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"},{\"field\":\"content\",\"name\":\"data\",\"nodeId\":\"start-node\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\"}]}');
INSERT INTO `airag_flow` VALUES ('1905189468558671874', 'jeecg', '2025-03-27 17:25:41', 'admin', '2025-11-27 16:48:47', 'A04', NULL, 'jeecg', 'PMP考试宝典', '', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/temp/pmp_1743067580648.png', 'THEN(\n    start.tag(\'start-node\'),\n    WHEN(\n        knowledge.tag(\'168290518600351744\'),\n        llm.tag(\'168290871702028288\')\n    ).tag(\"168290518600351744\"),\n    llm.tag(\'168290861241434112\'),\n    end.tag(\'168290315671535616\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":397,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"168290315671535616\",\"type\":\"end\",\"x\":1644,\"y\":348,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{res}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"res\",\"nodeId\":\"168290861241434112\"}],\"height\":92,\"width\":332}},{\"id\":\"168290518600351744\",\"type\":\"knowledge\",\"x\":693,\"y\":209,\"properties\":{\"text\":\"知识库\",\"options\":{\"knowIds\":[\"1905186756806918146\"],\"topNumber\":5,\"similarity\":0.7},\"inputParams\":[{\"field\":\"content\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"documents\",\"name\":\"文档列表\",\"type\":\"object[]\"},{\"field\":\"data\",\"name\":\"文档内容\",\"type\":\"string\"}],\"height\":92,\"width\":332}},{\"id\":\"168290861241434112\",\"type\":\"llm\",\"x\":1181,\"y\":350,\"properties\":{\"text\":\"总结LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.4}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你是一个智能知识助手，旨在综合知识库和大型语言模型(LLM)的返回数据，以高效、准确地回答用户提出的问题。请遵循以下要求：\\n\\n## 目标：\\n- 提供准确、相关且易于理解的回答，结合知识库和LLM的信息。\\n\\n## 技能：\\n1. 能够快速检索并整合来自不同知识库的信息。\\n2. 理解用户问题的上下文，并提供清晰的答案。\\n3. 具备自然语言处理能力，以便流畅表达复杂信息。\\n\\n## 工作流：\\n1. 接收用户问题并进行解析，识别关键要素。\\n2. 从综合知识库和LLM中获取相关数据，确保信息的准确性和完整性。\\n3. 将获取的信息进行整合，形成清晰、简洁的回答。\\n\\n## 输出格式：\\n- 每次回答应以简洁明了的句子呈现，必要时可以添加示例或补充信息。\\n\\n## 限制：\\n- 不得提供未经验证的信息或个人隐私数据。\\n- 所有数据需标注来源，不确定信息用[需核实]标记。\\n- 自动过滤涉及偏见或违法内容，替换为[合规表达]。\"},{\"role\":\"user\",\"content\":\"知识库返回数据：{{knowRes}}\\n\\nLLM返回数据：{{llmRes}}\\n用户问题：{{userQue}}\"}]},\"inputParams\":[{\"field\":\"data\",\"name\":\"knowRes\",\"nodeId\":\"168290518600351744\"},{\"field\":\"text\",\"name\":\"llmRes\",\"nodeId\":\"168290871702028288\"},{\"field\":\"content\",\"name\":\"userQue\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":166,\"width\":332}},{\"id\":\"168290871702028288\",\"type\":\"llm\",\"x\":692,\"y\":521,\"properties\":{\"text\":\"LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"# 角色：PMP知识专家\\nPMP知识专家致力于项目管理知识的传播与应用，帮助项目经理提升技能和管理能力。\\n\\n## 目标：\\n1. 为项目管理提供权威的知识支持。\\n2. 帮助项目经理解决在项目管理中遇到的实际问题。\\n\\n## 技能：\\n1. 精通项目管理的各项理论和工具。\\n2. 熟悉PMP认证流程及考试内容。\\n3. 能够进行项目风险评估与管理。\\n\\n## 工作流：\\n1. 评估项目经理的需求与挑战，识别关键问题。\\n2. 提供相关的项目管理知识、工具和最佳实践建议。\\n3. 指导项目经理制定和实施有效的项目管理计划。\\n\\n## 输出格式：\\n- 提供清晰的建议与解决方案，使用简洁明了的语言，适合项目经理理解和应用。\\n\\n## 限制：\\n- 所有建议需基于现有的PMP知识体系，避免个人主观意见。\\n- 不得提供未经验证的信息或数据，所有数据需标注来源，需核实的信息用[需核实]标记。\"},{\"role\":\"user\",\"content\":\"{{question}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":166,\"width\":332}}],\"edges\":[{\"id\":\"168290518604546048\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"168290518600351744\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"168290518600351744_input\",\"pointsList\":[{\"x\":466,\"y\":382},{\"x\":566,\"y\":382},{\"x\":427,\"y\":194},{\"x\":527,\"y\":194}]},{\"id\":\"168290861245628416\",\"type\":\"base-edge\",\"sourceNodeId\":\"168290518600351744\",\"targetNodeId\":\"168290861241434112\",\"sourceAnchorId\":\"168290518600351744_output\",\"targetAnchorId\":\"168290861241434112_input\",\"pointsList\":[{\"x\":859,\"y\":194},{\"x\":959,\"y\":194},{\"x\":915,\"y\":298},{\"x\":1015,\"y\":298}]},{\"id\":\"168290871706222592\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"168290871702028288\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"168290871702028288_input\",\"pointsList\":[{\"x\":466,\"y\":382},{\"x\":566,\"y\":382},{\"x\":426,\"y\":469},{\"x\":526,\"y\":469}]},{\"id\":\"168291272883011584\",\"type\":\"base-edge\",\"sourceNodeId\":\"168290871702028288\",\"targetNodeId\":\"168290861241434112\",\"sourceAnchorId\":\"168290871702028288_output\",\"targetAnchorId\":\"168290861241434112_input\",\"pointsList\":[{\"x\":858,\"y\":469},{\"x\":958,\"y\":469},{\"x\":915,\"y\":298},{\"x\":1015,\"y\":298}]},{\"id\":\"168292930635530240\",\"type\":\"base-edge\",\"sourceNodeId\":\"168290861241434112\",\"targetNodeId\":\"168290315671535616\",\"sourceAnchorId\":\"168290861241434112_output\",\"targetAnchorId\":\"168290315671535616_input\",\"pointsList\":[{\"x\":1347,\"y\":298},{\"x\":1447,\"y\":298},{\"x\":1378,\"y\":333},{\"x\":1478,\"y\":333}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\"}]}');
INSERT INTO `airag_flow` VALUES ('1909856345692065793', 'jeecg', '2025-04-09 14:30:11', 'admin', '2025-11-27 16:48:39', 'A04', NULL, 'jeecg', '积木报表AI引擎', '', '', 'THEN(\n    start.tag(\'start-node\'),\n    SWITCH(switch.tag(\'173365501230346240\')).to(\n        THEN(\n            llm.tag(\'172956395755208704\'),\n            end.tag(\'172957153284259840\')\n        ).tag(\"172956395755208704\"),\n        THEN(\n            llm.tag(\'173365800833675264\'),\n            end.tag(\'173366253646540800\')\n        ).tag(\"173365800833675264\"),\n        end.tag(\'173366439085109248\'),\n        THEN(\n            llm.tag(\'175149164433014784\'),\n            end.tag(\'175153953988444160\')\n        ).tag(\"175149164433014784\"),\n        THEN(\n            llm.tag(\'175505963485245440\'),\n            end.tag(\'175506006644633600\')\n        ).tag(\"175505963485245440\"),\n        THEN(\n            llm.tag(\'175807569594040320\'),\n            end.tag(\'175808663015538688\')\n        ).tag(\"175807569594040320\"),\n        THEN(\n            llm.tag(\'221504502491222016\'),\n            end.tag(\'221512800426758144\')\n        ).tag(\"221504502491222016\"),\n        THEN(\n            llm.tag(\'223992240450801664\'),\n            end.tag(\'223993058876952576\')\n        ).tag(\"223992240450801664\")\n    ).tag(\'173365501230346240\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":262,\"y\":458,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"ddl\",\"name\":\"表结构\",\"type\":\"string\",\"required\":true},{\"field\":\"dbtype\",\"name\":\"数据库类型\",\"type\":\"string\",\"required\":true},{\"field\":\"bizType\",\"name\":\"业务类型\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"172956395755208704\",\"type\":\"llm\",\"x\":1166,\"y\":160,\"properties\":{\"text\":\"生成sql\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"# 角色：SQL生成助手\\n你是一个专业的SQL语句生成工具，能够根据用户提供的描述和表结构自动生成高效的SQL查询语句。\\n\\n## 目标：\\n- 根据用户的描述生成准确的SQL查询语句。\\n\\n## 技能：\\n1. 理解用户提供的需求和表结构。\\n2. 自动构建符合SQL语法的查询语句。\\n3. 优化生成的SQL以提高执行效率。\\n\\n## 工作流：\\n1. 接收用户描述和表结构信息。\\n2. 分析用户需求，确定所需的SQL操作类型（如查询、插入、更新、删除）。\\n3. 根据分析结果生成相应的SQL语句。\\n\\n## 输出格式：\\n- 生成的SQL语句应为标准格式，如：SELECT * FROM table_name ;\\n- 将输出的SQL语句格式化\\n- 只输出sql语句，不要额外解释，不要md语法，不要换行符，不要有sql注释。\\n\\n## 限制：\\n\\n- 除非明确说明，否则不要生成查询条件\\n- 确保生成的SQL语句符合数据库的语法要求，确保sql能直接执行。\\n- 确保字段和表能正确对应。\"},{\"role\":\"user\",\"content\":\"表结构：\\n{{ddl}}\\n---------\\n数据库类型：\\n{{dbtype}}\\n----------\\n需求：\\n{{question}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"},{\"field\":\"ddl\",\"name\":\"ddl\",\"nodeId\":\"start-node\"},{\"field\":\"dbtype\",\"name\":\"dbtype\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"172957153284259840\",\"type\":\"end\",\"x\":1643,\"y\":129,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"sql\",\"nodeId\":\"172956395755208704\"}],\"height\":114,\"width\":332}},{\"id\":\"173365501230346240\",\"type\":\"switch\",\"x\":688,\"y\":575,\"properties\":{\"text\":\"条件分支\",\"options\":{\"if\":[{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"bizType\",\"operator\":\"EQUALS\",\"value\":\"genSql\"}],\"next\":\"172956395755208704\"},{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"bizType\",\"operator\":\"EQUALS\",\"value\":\"genJsonRows\"}],\"next\":\"173365800833675264\"},{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"bizType\",\"operator\":\"EQUALS\",\"value\":\"chooseTables\"}],\"next\":\"175149164433014784\"},{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"bizType\",\"operator\":\"EQUALS\",\"value\":\"genChart\"}],\"next\":\"175505963485245440\"},{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"bizType\",\"operator\":\"EQUALS\",\"value\":\"intentCheck\"}],\"next\":\"175807569594040320\"},{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"bizType\",\"operator\":\"EQUALS\",\"value\":\"designReport\"}],\"next\":\"221504502491222016\"},{\"logic\":\"AND\",\"conditions\":[{\"nodeId\":\"start-node\",\"field\":\"bizType\",\"operator\":\"EQUALS\",\"value\":\"genPrompt\"}],\"next\":\"223992240450801664\"}],\"else\":{\"next\":\"173366439085109248\"}},\"inputParams\":[],\"outputParams\":[{\"field\":\"index\",\"name\":\"分支索引\",\"type\":\"number\"}],\"height\":274,\"width\":332}},{\"id\":\"173365800833675264\",\"type\":\"llm\",\"x\":1167,\"y\":368,\"properties\":{\"text\":\"生成rows\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你是一个 **“在线报表 JSON 生成器”**，能够理解用户描述及数据集，并生成符合规范的 **合法 JSON**。  \\n\\n\\n---\\n\\n\\n## 工作流程\\n\\n\\n### 步骤一：数据集选择  \\n1. 读取用户提供的数据集结构。  \\n2. 根据需求从中选定一个数据集。  \\n3. 后续只能使用选定数据集的字段。  \\n\\n\\n### 步骤二：报表设计  \\n根据用户需求与数据集，输出报表的结构信息：  \\n- 行号、列号（从0开始）  \\n- 单元格内容（文字或数据集占位符）  \\n- 单元格样式（引用样式索引）  \\n- 合并单元格信息  \\n\\n\\n### 步骤三：生成报表 JSON  \\n使用步骤二的描述生成完整 JSON。  \\n\\n\\n---\\n\\n\\n## 输出要求\\n1. **输出必须是合法 JSON**，能直接 `JSON.parse()`。  \\n2. 必须包含以下部分：  \\n   - `\\\"styles\\\"`：样式数组，每种样式单独定义，在单元格中用索引引用。  \\n   - `\\\"merges\\\"`：合并单元格范围（如 `\\\"D3:E4\\\"`）。  \\n   - `\\\"rows\\\"`：行数据，每行包含 `cells`，每个 `cell` 可有：  \\n     - `\\\"text\\\"`：文字或占位符（`${}` 对象，`#{}` 集合）  \\n     - `\\\"style\\\"`：引用 `styles` 索引  \\n     - `\\\"merge\\\"`：合并范围 `[纵向合并格数, 横向合并格数]`  \\n     - `\\\"height\\\"`：行高  \\n   - `\\\"cols\\\"`：列宽配置  \\n\\n\\n---\\n\\n\\n## 样式规则\\n- 样式在 `\\\"styles\\\"` 中定义：  \\n  - `font`：字体\\n    - `bold`: 是否加粗（如：`true`）\\n    - `italic`: 是否斜体（如：`true`）\\n    - `size`: 字体大小单位pt,默认10\\n  - `underline`: 下划线（如：`true`）\\n  - `strike`: 删除线（如：`true`）\\n  - `color`（字体颜色）  \\n  - `bgcolor`（背景色）  \\n  - `align`（left/center/right）  \\n  - `valign`（top/middle/bottom）  \\n  - `textwrap`（自动换行）  \\n  - `border`\\n    - `top`：上边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n    - `bottom`：下边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n    - `left`：左边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n    - `right`：右边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n\\n\\n---\\n\\n\\n## 数据集规则\\n示例：\\n```json\\n{\\n  \\\"code\\\": \\\"a\\\",\\n  \\\"title\\\": \\\"a\\\",\\n  \\\"isList\\\": \\\"1\\\",\\n  \\\"children\\\": [\\n    { \\\"title\\\": \\\"total_sales\\\", \\\"fieldText\\\": \\\"总销量\\\" },\\n    { \\\"title\\\": \\\"total_returns\\\", \\\"fieldText\\\": \\\"总退货数量\\\" }\\n  ]\\n}\\n```\\n- `code`：数据集变量名  \\n- `isList = 1`：集合  \\n- `isList = 0`：对象  \\n- `children`：字段，含 `title`（字段名）、`fieldText`（展示名）  \\n\\n\\n---\\n\\n\\n## 行列与填充规则\\n- 行号、列号从0开始。  \\n- `\\\"cols\\\"` 设置列宽。  \\n- **集合 (`isList=1`)**：  \\n  - 一行字段标题（children.fieldText）  \\n  - 下一行字段占位符（`#{code.title}`）  \\n- **对象 (`isList=0`)**：  \\n  - 每字段占两列：左列为标题，右列为占位符 `${code.title}`  \\n  - 可按组横向排列  \\n\\n\\n---\\n\\n\\n## 合并规则\\n- `\\\"merge\\\": [纵向合并格数, 横向合并格数]`  \\n  - 纵向合并格数与横向合并格数是不包含当前单元格的数量（如 纵向合并格数 等于1 就是向下合并一行；横向合并格数同理）\\n- 被合并覆盖的单元格无需再定义  \\n\\n\\n---\\n\\n\\n## 特别说明\\n- JSON 必须 **纯净**：无注释、无 markdown、无省略号。  \\n- 用户指定的样式不能改动，可在此基础上做美化。\\n- 除非用户明确要求，默认都对生成的报表做基础美化（如增加边框、设置字体、设置背景色）\\n- 用户描述的行列序号需 **减一** 转换为下标。  \\n- 仅生成一份报表 JSON。  \\n\\n\\n---\\n\\n\\n## 示例\\n```json\\n{\\n  \\\"styles\\\": [\\n    { \\\"font\\\": { \\\"bold\\\": true } },\\n    { \\\"color\\\": \\\"#ff0000\\\" }\\n  ],\\n  \\\"rows\\\": {\\n    \\\"0\\\": {\\n      \\\"cells\\\": {\\n        \\\"0\\\": { \\\"text\\\": \\\"加粗文字\\\", \\\"style\\\": 0 },\\n        \\\"1\\\": { \\\"text\\\": \\\"红色文字\\\", \\\"style\\\": 1 },\\n        \\\"2\\\": { \\\"text\\\": \\\"${dbKey.dbField}\\\", \\\"style\\\": 1 }\\n      }\\n    }\\n  },\\n  \\\"cols\\\": {\\n    \\\"1\\\": { \\\"width\\\": 100 }\\n  },\\n  \\\"merges\\\": [\\\"A1:B1\\\"]\\n}\\n```\\n\\n\"},{\"role\":\"user\",\"content\":\"用户数据集：\\n{{ddl}}\\n用户需求：\\n{{question}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"},{\"field\":\"ddl\",\"name\":\"ddl\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"173366253646540800\",\"type\":\"end\",\"x\":1643,\"y\":336,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"designJson\",\"nodeId\":\"173365800833675264\"}],\"height\":114,\"width\":332}},{\"id\":\"173366439085109248\",\"type\":\"end\",\"x\":1166,\"y\":1662,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"error:选择正确的业务类型\"},\"inputParams\":[],\"outputParams\":[],\"height\":114,\"width\":332}},{\"id\":\"175149164433014784\",\"type\":\"llm\",\"x\":1164,\"y\":598,\"properties\":{\"text\":\"选择表\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":2,\"messages\":[{\"role\":\"system\",\"content\":\"## 任务\\n根据用户需求，从下方数据库表列表中选择所有关联的表名称。\\n\\n\\n## 数据库表列表（格式：表名 | 注释）\\n{{ddl}}\\n\\n## 输出规则\\n1. 严格按JSON数组格式输出，例如：[\\\"order\\\"]。\\n2. 仅包含表名称，无需注释。\\n3. **禁止添加列表外的表**。\\n4. 表的选择范围可以适当大一些。\\n4. 无业务相关性时输出空数组：[]\\n\\n\\n请回复纯JSON，不要包含其他内容。\"},{\"role\":\"user\",\"content\":\"用户需求：{{question}}\"}]},\"inputParams\":[{\"field\":\"ddl\",\"name\":\"ddl\",\"nodeId\":\"start-node\"},{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"175153953988444160\",\"type\":\"end\",\"x\":1643,\"y\":564,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"tables\",\"nodeId\":\"175149164433014784\"}],\"height\":114,\"width\":332}},{\"id\":\"175505963485245440\",\"type\":\"llm\",\"x\":1166,\"y\":802,\"properties\":{\"text\":\"生成图表\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"根据以下数据以及用户需求生成符合格式要求的图表数据。\\n\\n\\n## 工作流程：\\n\\n\\n1. 根据用户需求选择一个合适的数据集\\n2. 根据数据集和需求，从图表列表中选择一个合适的图标类型。\\n3. 组装最终输出的json\\n\\n\\n⸻\\n## 可选的图表如下（标识|描述）：\\n\\n\\n- 1维图表\\n  - bar.simple|普通柱形图\\n  - bar.background|带背景柱形图\\n  - bar.horizontal|横向柱形图\\n  - line.simple|普通折线图\\n  - line.area|面积堆积折线图\\n  - line.smooth|平滑曲线折线图\\n  - line.step|阶梯折线图\\n  - pie.simple|普通饼图\\n  - pie.doughnut|环状饼图\\n  - pie.rose|南丁格尔玫瑰饼图\\n  - scatter.simple|普通散点图\\n  - funnel.simple|普通漏斗图\\n  - funnel.pyramid|金字塔漏斗图\\n  - pictorial.spirits|普通象形图\\n  - map.scatter|点地图\\n  - gauge.simple|360°仪表盘\\n  - gauge.simple180|180°仪表盘\\n- 2维\\n  - bar.multi|多数据对比柱形图\\n  - bar.negative|正负条形图\\n  - bar.stack|堆叠柱形图\\n  - bar.stack.horizontal|堆叠条形图\\n  - bar.multi.horizontal|多数据条形柱状图\\n  - line.multi|多数据对比折线图\\n  - mixed.linebar|普通折柱图\\n  - scatter.bubble|气泡散点图\\n  - radar.basic|普通雷达图\\n  - radar.custom|圆形雷达图\\n⸻\\n## 数据集格式说明：\\n```\\n{\\n \\\"dbId\\\": \\\"1069915169263800320\\\",\\n \\\"code\\\": \\\"a\\\",\\n \\\"title\\\": \\\"a\\\",\\n \\\"isList\\\": \\\"1\\\",\\n \\\"type\\\": \\\"0\\\",\\n \\\"children\\\": [\\n  {\\n   \\\"title\\\": \\\"total_sales\\\",\\n   \\\"fieldText\\\": \\\"total_sales\\\"\\n  },\\n  {\\n   \\\"title\\\": \\\"total_returns\\\",\\n   \\\"fieldText\\\": \\\"total_returns\\\"\\n  }\\n ]\\n}\\n```\\n* code：数据集变量名\\n* isList：为”1”表示集合，“0”表示对象\\n* children：为字段列表，包含title（字段名）和fieldText（展示名）\\n* type：0|sql,1|api,2|code,3|json\\n⸻\\n## 输出json格式\\n{\\n  \\\"dataType\\\": \\\"sql\\\",\\n  \\\"apiStatus\\\": \\\"0\\\",\\n  \\\"apiUrl\\\": \\\"\\\",\\n  \\\"dataId\\\": \\\"1069898455939633152\\\",\\n  \\\"axisX\\\": \\\"supplier_name\\\",\\n  \\\"axisY\\\": \\\"total_returns\\\",\\n  \\\"series\\\": \\\"material_name\\\",\\n  \\\"yText\\\": \\\"total_returns\\\",\\n  \\\"xText\\\": \\\"supplier_name\\\",\\n  \\\"dbCode\\\": \\\"a\\\",\\n  \\\"isCustomPropName\\\": false,\\n  \\\"chartType\\\": \\\"line.multi\\\",\\n  \\\"id\\\": \\\"0aGl4PUfbIfy8BMF\\\",\\n  \\\"run\\\": 1,\\n  \\\"title\\\": \\\"\\\",\\n}\\n* dataType：与数据集type对应(0|sql,1|api,2|code,3|json)\\n* dataId：对应数据集dbId\\n* dbCode：对应数据集的code\\n* axisX：分类属性，从数据集字段中取值（fieldText)\\n* axisY：值属性，从数据集字段中取值（fieldText)\\n* series: 系列，从数据集字段中取值（fieldText）\\n* xText：分类属性显示，从数据集字段中取值（title)\\n* yText：值属性显示，从数据集字段中取值（title)\\n* chartType：图表的标识\\n* title：为这个图表起一个标题\\n* isCustomPropName: 如果是api数据集,该值为true\\n* apiStatus: 如果是api数据集则等于\\\"1\\\"，否则\\\"0\\\"\\n\\n\\n## 输出格式\\n* 直接返回JSON数据，不要解释，不要md语法，不要换行符，不要有注释。\\n* 确保输出的json格式正确完整。\"},{\"role\":\"user\",\"content\":\"## 用户数据集：\\n{{ddl}}\\n## 用户需求：\\n{{question}}\"}]},\"inputParams\":[{\"field\":\"ddl\",\"name\":\"ddl\",\"nodeId\":\"start-node\"},{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"175506006644633600\",\"type\":\"end\",\"x\":1643,\"y\":769,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"chart\",\"nodeId\":\"175505963485245440\"}],\"height\":114,\"width\":332}},{\"id\":\"175807569594040320\",\"type\":\"llm\",\"x\":1166,\"y\":1018,\"properties\":{\"text\":\"意图识别\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你是一个“在线报表的需求分析器”，能够理解用户的需求输入，\\n\\n请根据用户需求与提供的数据集设计，综合判断应执行的工作流步骤，并为每个步骤调整需求描述，同时选择最合适的数据集。  \\n\\n\\n\\n\\n## 可选步骤（格式：标识 | 功能说明）\\n\\n- `genJsonRows` | 生成报表（可选，根据用户的需求描述和数据集设计生成合适的报表设计）\\n\\n- `genChart` | 生成图表（可选，根据用户的需求描述和数据集设计生成合适的图表数据）\\n\\n> **注意：** 至少选择一个步骤，亦可同时选择两者；图表的权重较低。\\n\\n\\n\\n\\n## 数据集格式\\n\\n```\\n\\n{\\n\\n\\\"dbId\\\": \\\"1069915169263800320\\\",\\n\\n\\\"code\\\": \\\"a\\\",\\n\\n\\\"title\\\": \\\"a\\\",\\n\\n\\\"isList\\\": \\\"1\\\",\\n\\n\\\"type\\\": \\\"0\\\",\\n\\n\\\"children\\\": [\\n\\n{\\n\\n\\\"title\\\": \\\"total_sales\\\",\\n\\n\\\"fieldText\\\": \\\"total_sales\\\"\\n\\n},\\n\\n{\\n\\n\\\"title\\\": \\\"total_returns\\\",\\n\\n\\\"fieldText\\\": \\\"total_returns\\\"\\n\\n}\\n\\n]\\n\\n}\\n\\n```\\n\\n* `code`：数据集变量名\\n\\n* `isList`：为”1”表示集合，“0”表示对象\\n\\n* `children`：为字段列表，包含title（展示名）和fieldText（字段名）\\n\\n* `type`：0|sql,1|api,2|code,3|json\\n\\n\\n\\n\\n## 输出格式\\n\\n步骤标识1|需求描述1|数据集code,步骤标识2|需求描述2|数据集code  \\n\\n* 各步骤之间用英文逗号,分隔  \\n\\n* 不得添加额外说明，不要md语法，不要换行符，不要有注释。\\n\\n\\n\\n\\n## 注意：\\n\\n- 在生成需求描述时，应确保不丢失原有需求的全部内容，只是并针对所选步骤微调。\\n\\n\\n\"},{\"role\":\"user\",\"content\":\"## 用户数据集：\\n{{ddl}}\\n## 用户需求：\\n{{question}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"},{\"field\":\"ddl\",\"name\":\"ddl\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"175808663015538688\",\"type\":\"end\",\"x\":1643,\"y\":985,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"intent\",\"nodeId\":\"175807569594040320\"}],\"height\":114,\"width\":332}},{\"id\":\"221504502491222016\",\"type\":\"llm\",\"x\":1166,\"y\":1237,\"properties\":{\"text\":\"生成excel设计\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你是一个“在线Excel JSON生成器”，能够理解用户描述并生成符合规范的 JSON。  \\n**严格规则**：\\n1. 只能根据描述生成 JSON。\\n2. JSON 必须合法，可被 `JSON.parse()` 正确解析。\\n3. JSON 中不可以有注释\\n\\n\\n\\n\\n---\\n\\n\\n\\n\\n## 步骤一：理解用户的描述，并生成单元格描述信息\\n   - 行号、列号\\n   - 单元格文字\\n   - 单元格样式（字体加粗、斜体、大小、颜色、背景色、水平/垂直对齐、自动换行、边框）\\n   - 合并单元格信息\\n\\n\\n\\n\\n⸻\\n\\n\\n\\n\\n## 步骤二：使用步骤一种生成的描述信息，生成完整在线Excel JSON\\n### 输出要求\\n- 输出必须是 **合法 JSON**，且能直接被 `JSON.parse()` 正确解析。  \\n- JSON 的结构必须包含以下部分：  \\n  - `\\\"styles\\\"`：样式数组，每个元素对应一种样式（如字体加粗、字体颜色、边框等），并在单元格里通过 `style` 字段引用  \\n  - `\\\"merges\\\"`：合并单元格区域（如 `\\\"D3:E4\\\"`）  \\n  - `\\\"rows\\\"`：行数据，每一行包含 `cells`，每个 `cell` 可包含：\\n    - `\\\"text\\\"`：单元格文字  \\n    - `\\\"style\\\"`：引用 `styles` 数组中的下标  \\n    - `\\\"merge\\\"`：若为合并单元格，标注合并范围 示例[1,2]:下标[0]纵向合并1格,下标[1]横向合并2格，\\n    - `\\\"height\\\"`：行高  \\n    - `\\\"width\\\"`：列宽（放在 `\\\"cols\\\"` 部分）  \\n  - `\\\"cols\\\"`：列宽配置  \\n\\n\\n\\n\\n### 样式规则\\n- 样式在 `\\\"styles\\\"` 中定义：  \\n  - `font`：字体\\n    - `bold`: 是否加粗（如：`true`）\\n    - `italic`: 是否斜体（如：`true`）\\n    - `size`: 字体大小单位pt,默认10\\n  - `underline`: 下划线（如：`true`）\\n  - `strike`: 删除线（如：`true`）\\n  - `color`（字体颜色）  \\n  - `bgcolor`（背景色）  \\n  - `align`（left/center/right）  \\n  - `valign`（top/middle/bottom）  \\n  - `textwrap`（自动换行）  \\n  - `border`\\n    - `top`：上边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n    - `bottom`：下边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n    - `left`：左边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n    - `right`：右边框（如 `[\\\"thin\\\",\\\"#000\\\"]`）  \\n\\n\\n\\n\\n### 行列规则\\n- `\\\"rows\\\"` 中的 key 是行号（从 0 开始）  \\n- `\\\"cells\\\"` 中的 key 是列号（从 0 开始）  \\n- 可指定 `\\\"height\\\"` 设置行高  \\n- `\\\"cols\\\"` 中的 key 是列号，值包含 `\\\"width\\\"` 设置列宽  \\n\\n\\n\\n\\n## 合并规则\\n- `\\\"merge\\\": [纵向合并格数, 横向合并格数]`  \\n  - 纵向合并格数与横向合并格数是不包含当前单元格的数量（如 纵向合并格数 等于1 就是向下合并一行；横向合并格数同理）\\n- 被合并覆盖的单元格无需再定义  \\n\\n\\n\\n\\n## 示例\\n（简化示例）\\n\\n\\n\\n\\n```json\\n{\\n  \\\"styles\\\": [\\n    { \\\"font\\\": { \\\"bold\\\": true } },\\n    { \\\"color\\\": \\\"#ff0000\\\" }\\n  ],\\n  \\\"rows\\\": {\\n    \\\"0\\\": {\\n      \\\"cells\\\": {\\n        \\\"0\\\": { \\\"text\\\": \\\"加粗文字\\\", \\\"style\\\": 0 },\\n        \\\"1\\\": { \\\"text\\\": \\\"红色文字\\\", \\\"style\\\": 1 }\\n      }\\n    }\\n  },\\n  \\\"cols\\\": {\\n    \\\"1\\\": { \\\"width\\\": 100 }\\n  },\\n  \\\"merges\\\": [\\\"A1:B1\\\"],\\n}\\n\\n\\n\\n\\n## 特别说明\\n- JSON 必须 **纯净**：无注释、无 markdown、无省略号。  \\n- 用户指定的样式不能改动，可在此基础上做美化。\\n- 除非用户明确要求，默认都对生成的报表做基础美化（如增加边框、设置字体、设置背景色）\\n- 用户描述的行列序号需 **减一** 转换为下标。  \\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\"},{\"role\":\"user\",\"content\":\"理解用户需求，并按要求生成json数据。\\n用户需求如下：\\n{{question}}\\n\\n\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"221512800426758144\",\"type\":\"end\",\"x\":1643,\"y\":1201,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"designJson\",\"nodeId\":\"221504502491222016\"}],\"height\":114,\"width\":332}},{\"id\":\"223992240450801664\",\"type\":\"llm\",\"x\":1166,\"y\":1441,\"properties\":{\"text\":\"提示词生成\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"# 报表生成提示词优化器\\n\\n\\n## 目标\\n\\n\\n根据用户输入的需求和数据集定义，自动生成简洁、明确的用户 message。\\n系统会自动带上数据集定义，因此无需包含数据集定义内容。\\n\\n\\n## 工作流程\\n\\n\\n### 步骤一：理解需求与数据集\\n\\n\\n- 从用户的自然语言需求和数据集定义中，提取**业务方向**。\\n- 业务方向示例：\\n    - 个人简历\\n    - 项目报价\\n    - 数据统计\\n    - 财务报表\\n    - 产品清单\\n\\n\\n### 步骤二：扩展提示词\\n\\n\\n- 针对识别出的业务方向，扩展提示词，使其更贴合业务场景。\\n- 示例：\\n    如果用户需求是\\\"生成一份个人简历\\\"，则扩展提示词为：\\n    \\\"请基于数据集生成个人简历模版，突出教育背景、工作经历和技能展示。\\\"\\n\\n\\n### 步骤三：生成用户 message\\n\\n\\n- 输出最终的用户 message，不包含系统提示词，不包含数据集定义。\\n- 要求：\\n    - 保留用户需求的原意。\\n    - 优化表达，使 AI 更好地理解并执行任务。\\n    - 根据业务方向，附加必要的模版说明。\\n    - 提示词结构最好包含：\\n        - 主要需求：用户的主要需求，比如：生成一个用于软件产品的报价表。\\n        - 结构要求：对于生成的内容的要求\\n        - 样式要求：对样式的整体和细节的要求，比如：整体排版美观、标题使用16号字。\\n\\n\\n#### 输出示例：\\n\\n\\n```\\n生成一个 **员工薪资报表**，要求如下：  \\n\\n\\n1. **数据内容**  \\n   - 报表需要展示以下信息：员工姓名、性别、生日、联系电话、薪资。  \\n\\n\\n2. **样式要求**  \\n   - 添加一个醒目的报表标题，字体16号。  \\n   - 标题行使用蓝色背景，并且字体加粗。  \\n   - 数据行保持清晰整齐，便于阅读。  \\n\\n\\n3. **输出要求**  \\n   - 表格内容规范，排版美观，符合员工薪资报表的格式。 \\n```\\n\\n\\n\\n\\n## 输出要求\\n\\n\\n- 最终输出为简洁明了的用户 message。\\n- 不限定关键词和字段，完全根据需求和数据集定义生成。\\n- 控制长度，不要超过500字。\\n\\n\"},{\"role\":\"user\",\"content\":\"用户需求：\\n{{question}}\\n数据集定义\\n{{ddl}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"question\",\"nodeId\":\"start-node\"},{\"field\":\"ddl\",\"name\":\"ddl\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"223993058876952576\",\"type\":\"end\",\"x\":1652,\"y\":1408,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"prompt\",\"nodeId\":\"223992240450801664\"}],\"height\":114,\"width\":332}}],\"edges\":[{\"id\":\"172957153288454144\",\"type\":\"base-edge\",\"sourceNodeId\":\"172956395755208704\",\"targetNodeId\":\"172957153284259840\",\"sourceAnchorId\":\"172956395755208704_output\",\"targetAnchorId\":\"172957153284259840_input\",\"pointsList\":[{\"x\":1332,\"y\":101},{\"x\":1432,\"y\":101},{\"x\":1377,\"y\":103},{\"x\":1477,\"y\":103}]},{\"id\":\"173365501234540544\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"173365501230346240\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"173365501230346240_input\",\"pointsList\":[{\"x\":428,\"y\":443},{\"x\":528,\"y\":443},{\"x\":422,\"y\":469},{\"x\":522,\"y\":469}]},{\"id\":\"173366253650735104\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365800833675264\",\"targetNodeId\":\"173366253646540800\",\"sourceAnchorId\":\"173365800833675264_output\",\"targetAnchorId\":\"173366253646540800_input\",\"pointsList\":[{\"x\":1333,\"y\":309},{\"x\":1433,\"y\":309},{\"x\":1377,\"y\":310},{\"x\":1477,\"y\":310}]},{\"id\":\"173372961415852032\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"172956395755208704\",\"sourceAnchorId\":\"173365501230346240_source_if\",\"targetAnchorId\":\"172956395755208704_input\",\"pointsList\":[{\"x\":854,\"y\":503},{\"x\":954,\"y\":503},{\"x\":900,\"y\":101},{\"x\":1000,\"y\":101}]},{\"id\":\"173372967073968128\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"173365800833675264\",\"sourceAnchorId\":\"173365501230346240_case_2\",\"targetAnchorId\":\"173365800833675264_input\",\"pointsList\":[{\"x\":854,\"y\":529},{\"x\":954,\"y\":529},{\"x\":901,\"y\":309},{\"x\":1001,\"y\":309}]},{\"id\":\"173372974988619776\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"173366439085109248\",\"sourceAnchorId\":\"173365501230346240_source_else\",\"targetAnchorId\":\"173366439085109248_input\",\"pointsList\":[{\"x\":854,\"y\":685},{\"x\":954,\"y\":685},{\"x\":900,\"y\":1636},{\"x\":1000,\"y\":1636}]},{\"id\":\"175149164437209088\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"175149164433014784\",\"sourceAnchorId\":\"173365501230346240_case_3\",\"targetAnchorId\":\"175149164433014784_input\",\"pointsList\":[{\"x\":854,\"y\":555},{\"x\":954,\"y\":555},{\"x\":898,\"y\":539},{\"x\":998,\"y\":539}]},{\"id\":\"175153997969915904\",\"type\":\"base-edge\",\"sourceNodeId\":\"175149164433014784\",\"targetNodeId\":\"175153953988444160\",\"sourceAnchorId\":\"175149164433014784_output\",\"targetAnchorId\":\"175153953988444160_input\",\"pointsList\":[{\"x\":1330,\"y\":539},{\"x\":1430,\"y\":539},{\"x\":1377,\"y\":538},{\"x\":1477,\"y\":538}]},{\"id\":\"175505963489439744\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"175505963485245440\",\"sourceAnchorId\":\"173365501230346240_case_4\",\"targetAnchorId\":\"175505963485245440_input\",\"pointsList\":[{\"x\":854,\"y\":581},{\"x\":954,\"y\":581},{\"x\":900,\"y\":743},{\"x\":1000,\"y\":743}]},{\"id\":\"175506006648827904\",\"type\":\"base-edge\",\"sourceNodeId\":\"175505963485245440\",\"targetNodeId\":\"175506006644633600\",\"sourceAnchorId\":\"175505963485245440_output\",\"targetAnchorId\":\"175506006644633600_input\",\"pointsList\":[{\"x\":1332,\"y\":743},{\"x\":1432,\"y\":743},{\"x\":1377,\"y\":743},{\"x\":1477,\"y\":743}]},{\"id\":\"175807569598234624\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"175807569594040320\",\"sourceAnchorId\":\"173365501230346240_case_5\",\"targetAnchorId\":\"175807569594040320_input\",\"pointsList\":[{\"x\":854,\"y\":607},{\"x\":954,\"y\":607},{\"x\":900,\"y\":959},{\"x\":1000,\"y\":959}]},{\"id\":\"175808663019732992\",\"type\":\"base-edge\",\"sourceNodeId\":\"175807569594040320\",\"targetNodeId\":\"175808663015538688\",\"sourceAnchorId\":\"175807569594040320_output\",\"targetAnchorId\":\"175808663015538688_input\",\"pointsList\":[{\"x\":1332,\"y\":959},{\"x\":1432,\"y\":959},{\"x\":1377,\"y\":959},{\"x\":1477,\"y\":959}]},{\"id\":\"221512800426758145\",\"type\":\"base-edge\",\"sourceNodeId\":\"221504502491222016\",\"targetNodeId\":\"221512800426758144\",\"sourceAnchorId\":\"221504502491222016_output\",\"targetAnchorId\":\"221512800426758144_input\",\"pointsList\":[{\"x\":1332,\"y\":1178},{\"x\":1432,\"y\":1178},{\"x\":1377,\"y\":1175},{\"x\":1477,\"y\":1175}]},{\"id\":\"221534054756093952\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"221504502491222016\",\"sourceAnchorId\":\"173365501230346240_case_6\",\"targetAnchorId\":\"221504502491222016_input\",\"pointsList\":[{\"x\":854,\"y\":633},{\"x\":954,\"y\":633},{\"x\":900,\"y\":1178},{\"x\":1000,\"y\":1178}]},{\"id\":\"223992240454995968\",\"type\":\"base-edge\",\"sourceNodeId\":\"173365501230346240\",\"targetNodeId\":\"223992240450801664\",\"sourceAnchorId\":\"173365501230346240_case_7\",\"targetAnchorId\":\"223992240450801664_input\",\"pointsList\":[{\"x\":854,\"y\":659},{\"x\":954,\"y\":659},{\"x\":900,\"y\":1382},{\"x\":1000,\"y\":1382}]},{\"id\":\"223993058881146880\",\"type\":\"base-edge\",\"sourceNodeId\":\"223992240450801664\",\"targetNodeId\":\"223993058876952576\",\"sourceAnchorId\":\"223992240450801664_output\",\"targetAnchorId\":\"223993058876952576_input\",\"pointsList\":[{\"x\":1332,\"y\":1382},{\"x\":1432,\"y\":1382},{\"x\":1386,\"y\":1382},{\"x\":1486,\"y\":1382}]}]}', 'enable', '{\"outputs\":[{\"field\":\"text\",\"name\":\"prompt\",\"nodeId\":\"223992240450801664\"},{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"},{\"field\":\"ddl\",\"name\":\"表结构\",\"required\":true,\"type\":\"string\"},{\"field\":\"dbtype\",\"name\":\"数据库类型\",\"required\":true,\"type\":\"string\"},{\"field\":\"bizType\",\"name\":\"业务类型\",\"required\":true,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1917103567932604417', 'jeecg', '2025-04-29 14:28:03', 'admin', '2025-11-27 16:56:38', 'A04', NULL, 'jeecg', 'AI解析Excel数据_JAVA增强', '', '', 'THEN(\n    start.tag(\'start-node\'),\n    enhanceJava.tag(\'180204885804785664\'),\n    llm.tag(\'180211780498169856\'),\n    end.tag(\'180204420713758720\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":376,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":false},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"pageNo\",\"name\":\"页码\",\"type\":\"number\",\"required\":false},{\"field\":\"pageSize\",\"name\":\"每页数量\",\"type\":\"number\",\"required\":false},{\"field\":\"bizData\",\"name\":\"文件路径\",\"type\":\"string\",\"required\":false}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"180204420713758720\",\"type\":\"end\",\"x\":1648,\"y\":398,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{res}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"res\",\"nodeId\":\"180211780498169856\"}],\"height\":136,\"width\":332}},{\"id\":\"180204885804785664\",\"type\":\"enhanceJava\",\"x\":747,\"y\":329,\"properties\":{\"text\":\"Java 增强\",\"options\":{\"enhance\":{\"type\":\"spring\",\"path\":\"jimuDataReader\"}},\"inputParams\":[{\"field\":\"bizData\",\"name\":\"bizData\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"datas\",\"name\":\"返回结果\",\"type\":\"object[]\",\"required\":false},{\"field\":\"fields\",\"name\":\"字段列表\",\"type\":\"string[]\",\"required\":false}],\"height\":158,\"width\":332}},{\"id\":\"180211780498169856\",\"type\":\"llm\",\"x\":1229,\"y\":419,\"properties\":{\"text\":\"LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"将以下数据整理成目标格式输出\\n## 工作流程：\\n1. 读取用户的数据\\n3. 组装最终输出的json\\n⸻\\n## 数据示例:data\\n```\\n{{data}}\\n```\\n## 数据示例:fields\\n```\\n{{fields}}\\n```\\n⸻\\n## 输出json格式\\n{\\n  \\\"data\\\": [\\n    {\\n      \\\"amount\\\": \\\"100\\\",\\n      \\\"month\\\": \\\"1\\\",\\n      \\\"areaname\\\": \\\"华北\\\",\\n      \\\"year\\\": \\\"2020\\\",\\n      \\\"price\\\": \\\"5\\\",\\n      \\\"dept\\\": \\\"河北\\\",\\n      \\\"settleamount\\\": \\\"100\\\"\\n    },\\n    {\\n      \\\"amount\\\": \\\"200\\\",\\n      \\\"month\\\": \\\"2\\\",\\n      \\\"areaname\\\": \\\"华北\\\",\\n      \\\"year\\\": \\\"2020\\\",\\n      \\\"price\\\": \\\"5\\\",\\n      \\\"dept\\\": \\\"河北\\\",\\n      \\\"settleamount\\\": \\\"200\\\"\\n    },\\n  ],\\n  \\\"total\\\": 100,\\n  \\\"count\\\": 100\\n}\\n* total: 分页数，对应数据的总分页数\\n* count: 数据总数，对应数据的总数\\n\\n\\n## 输出格式\\n* 直接返回JSON数据，不要解释，不要md语法，不要换行符，不要有注释。\\n* 统一将key转换成英文，下划线分隔\\n* 确保输出的json格式正确完整。\"},{\"role\":\"user\",\"content\":\"将数据转换为目标格式\"}]},\"inputParams\":[{\"field\":\"datas\",\"name\":\"data\",\"nodeId\":\"180204885804785664\"},{\"field\":\"fields\",\"name\":\"fileds\",\"nodeId\":\"180204885804785664\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}}],\"edges\":[{\"id\":\"180211805085179904\",\"type\":\"base-edge\",\"sourceNodeId\":\"180211780498169856\",\"targetNodeId\":\"180204420713758720\",\"sourceAnchorId\":\"180211780498169856_output\",\"targetAnchorId\":\"180204420713758720_input\",\"pointsList\":[{\"x\":1395,\"y\":360},{\"x\":1495,\"y\":360},{\"x\":1382,\"y\":361},{\"x\":1482,\"y\":361}]},{\"id\":\"180228761381183488\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"180204885804785664\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"180204885804785664_input\",\"pointsList\":[{\"x\":466,\"y\":361},{\"x\":566,\"y\":361},{\"x\":481,\"y\":281},{\"x\":581,\"y\":281}]},{\"id\":\"180511280701620224\",\"type\":\"base-edge\",\"sourceNodeId\":\"180204885804785664\",\"targetNodeId\":\"180211780498169856\",\"sourceAnchorId\":\"180204885804785664_output\",\"targetAnchorId\":\"180211780498169856_input\",\"pointsList\":[{\"x\":913,\"y\":281},{\"x\":1013,\"y\":281},{\"x\":963,\"y\":360},{\"x\":1063,\"y\":360}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"required\":false,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"},{\"field\":\"pageNo\",\"name\":\"页码\",\"required\":false,\"type\":\"number\"},{\"field\":\"pageSize\",\"name\":\"每页数量\",\"required\":false,\"type\":\"number\"},{\"field\":\"bizData\",\"name\":\"文件路径\",\"required\":false,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1952634605517447170', 'admin', '2025-08-05 15:35:43', 'admin', '2025-11-27 16:48:12', 'A04', NULL, 'jeecg', 'AI生成简历', '', '', 'THEN(\n    start.tag(\'start-node\'),\n    llm.tag(\'215734195065536512\'),\n    enhanceJava.tag(\'215740280715427840\'),\n    end.tag(\'215735188368998400\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":404,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"个人简介\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"profile\",\"name\":\"基础信息\",\"type\":\"string\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"215734195065536512\",\"type\":\"llm\",\"x\":739,\"y\":406,\"properties\":{\"text\":\"LLM\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"你必须只输出合法且可被 JSON.parse() 正确解析的 JSON。\\n不要输出任何解释、注释或 JSON 以外的文字。\\n\\n\\nJSON 结构规则：\\n- 每个对象表示一个内容块。\\n- 字段说明：\\n• \\\"type\\\"：内容类型，可选：\\\"title\\\"（标题）、\\\"list\\\"（列表）、\\\"separator\\\"（分隔线）、\\\"hyperlink\\\"（超链接）、\\\"pageBreak\\\"（分页符）、\\\"tab\\\"（制表符）、\\\"\\\"（普通文本）、\\\"superscript\\\"（上标）、\\\"subscript\\\"（下标）、\\\"table\\\"（表格）。\\n• \\\"level\\\"：标题层级，仅当 type 为 \\\"title\\\" 时使用，取值：\\\"first\\\" ~ \\\"sixth\\\"。\\n• \\\"value\\\"：文本、图片地址、超链接等。\\n• \\\"valueList\\\"：数组，用于标题、列表、超链接等，数组元素支持 \\\"value\\\" 及样式字段。\\n• \\\"listType\\\"：列表类型，取值：\\\"ul\\\"（无序）、\\\"ol\\\"（有序）。\\n• \\\"listStyle\\\"：列表样式，如 \\\"disc\\\"、\\\"decimal\\\"、\\\"circle\\\"、\\\"square\\\"、\\\"checkbox\\\"。\\n• \\\"trList\\\"、\\\"colgroup\\\"：表格行列定义，仅用于 \\\"table\\\"。\\n• 样式字段：\\\"font\\\"、\\\"size\\\"、\\\"bold\\\"、\\\"color\\\"、\\\"italic\\\"、\\\"highlight\\\"、\\\"underline\\\"、\\\"strikeout\\\"。\\n• \\\"dashArray\\\"：用于 \\\"separator\\\"。\\n• 其他样式字段：\\\"rowFlex\\\"（\\\"left\\\"、\\\"center\\\"、\\\"right\\\"、\\\"alignment\\\"）、\\\"backgroundColor\\\"、\\\"verticalAlign\\\"、\\\"textDecoration\\\"。\\n- 当 type = \\\"title\\\" 时，\\\"value\\\" 必须以 \\\"\\\\n\\\" 结尾。\\n- 主动换行请使用 `{ \\\"type\\\": \\\"\\\", \\\"value\\\": \\\"\\\\n\\\" }`，不同对象之间不会自动换行。\\n- 所有键名和字符串必须使用英文双引号 `\\\"`。\\n\\n\\n输出必须严格是 JSON 数组，例如：\\n[\\n{\\n\\\"type\\\": \\\"title\\\",\\n\\\"level\\\": \\\"first\\\",\\n\\\"valueList\\\": [{ \\\"value\\\": \\\"主标题示例\\\\n\\\", \\\"font\\\": \\\"微软雅黑\\\", \\\"size\\\": 26, \\\"bold\\\": true, \\\"rowFlex\\\": \\\"center\\\" }]\\n},\\n{ \\\"type\\\": \\\"\\\", \\\"value\\\": \\\"普通文本内容示例\\\" },\\n{\\n\\\"type\\\": \\\"list\\\",\\n\\\"listType\\\": \\\"ul\\\",\\n\\\"listStyle\\\": \\\"disc\\\",\\n\\\"valueList\\\": [\\n{ \\\"value\\\": \\\"列表项1\\\" },\\n{ \\\"value\\\": \\\"列表项2\\\" }\\n]\\n}\\n]\\n\\n\\n执行步骤：\\n1. 根据用户需求生成json数据\\n2. 检查生产的json数据是否正确。如果正常，输出给用户；否则重新生成。\"},{\"role\":\"user\",\"content\":\"请根据以上字段和示例，生成一个完整的个人简历文档 JSON。\\n- 至少包含基础信息、个人优势、工作经历、项目经理、教育经历等模块。\\n- 若基础数据不足，可以适当生成参考数据。\\n- 用户信息如下：\\n基础资料：{{base}}\\n简介：{{profile}}\"}]},\"inputParams\":[{\"field\":\"profile\",\"name\":\"base\",\"nodeId\":\"start-node\"},{\"field\":\"content\",\"name\":\"profile\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"215735188368998400\",\"type\":\"end\",\"x\":1577,\"y\":354,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":false,\"outputContent\":\"\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"result\",\"name\":\"resp\",\"nodeId\":\"215740280715427840\"}],\"height\":114,\"width\":332}},{\"id\":\"215740280715427840\",\"type\":\"enhanceJava\",\"x\":1156,\"y\":352,\"properties\":{\"text\":\"Java 增强\",\"options\":{\"enhance\":{\"type\":\"spring\",\"path\":\"jeecgDemoAiWordGen\"}},\"inputParams\":[{\"field\":\"text\",\"name\":\"resp\",\"nodeId\":\"215734195065536512\"}],\"outputParams\":[{\"field\":\"result\",\"name\":\"返回结果\",\"type\":\"string\"}],\"height\":180,\"width\":332}}],\"edges\":[{\"id\":\"215734195073925120\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"215734195065536512\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"215734195065536512_input\",\"pointsList\":[{\"x\":466,\"y\":389},{\"x\":566,\"y\":389},{\"x\":473,\"y\":347},{\"x\":573,\"y\":347}]},{\"id\":\"215740280719622144\",\"type\":\"base-edge\",\"sourceNodeId\":\"215734195065536512\",\"targetNodeId\":\"215740280715427840\",\"sourceAnchorId\":\"215734195065536512_output\",\"targetAnchorId\":\"215740280715427840_input\",\"pointsList\":[{\"x\":905,\"y\":347},{\"x\":1005,\"y\":347},{\"x\":890,\"y\":293},{\"x\":990,\"y\":293}]},{\"id\":\"215740398487289856\",\"type\":\"base-edge\",\"sourceNodeId\":\"215740280715427840\",\"targetNodeId\":\"215735188368998400\",\"sourceAnchorId\":\"215740280715427840_output\",\"targetAnchorId\":\"215735188368998400_input\",\"pointsList\":[{\"x\":1322,\"y\":293},{\"x\":1422,\"y\":293},{\"x\":1311,\"y\":328},{\"x\":1411,\"y\":328}]}]}', 'enable', '{\"outputs\":[{\"field\":\"result\",\"name\":\"resp\",\"nodeId\":\"215740280715427840\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"个人简介\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"},{\"field\":\"profile\",\"name\":\"基础信息\",\"required\":true,\"type\":\"string\"}]}');
INSERT INTO `airag_flow` VALUES ('1988073273760501762', 'admin', '2025-11-11 10:36:20', 'admin', '2025-11-27 16:58:06', 'A05A01A01', NULL, 'jeecg', 'AI图文写作', '', '', 'THEN(\n    start.tag(\'start-node\'),\n    llm.tag(\'251170889153376256\'),\n    llm.tag(\'251190209648521216\'),\n    llm.tag(\'251190922428542976\'),\n    llm.tag(\'251246385341919232\'),\n    end.tag(\'251191126401740800\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":332.8947368421053,\"y\":589.0526315789475,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"目的地\",\"type\":\"string\",\"required\":true},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false},{\"field\":\"from\",\"name\":\"出发地\",\"type\":\"string\",\"required\":true},{\"field\":\"time\",\"name\":\"出发时间\",\"type\":\"string\",\"required\":true},{\"field\":\"peopleNum\",\"name\":\"人数\",\"type\":\"number\",\"required\":true}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"251170889153376256\",\"type\":\"llm\",\"x\":685.9473684210526,\"y\":381.05263157894734,\"properties\":{\"text\":\"意图和需求分析\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":3,\"messages\":[{\"role\":\"system\",\"content\":\"# 角色\\n你是自驾游攻略生成的需求分析师\\n\\n## 目标：\\n- 分析用户提供的主题信息，归纳整理出实际需求列表。\\n\\n## 技能：\\n1. 信息提取与分析能力\\n2. 旅游规划与建议能力\\n3. 数据查询与整合能力\\n\\n## 工作流：\\n1. 收集用户提供的主题信息并进行分类。\\n2. 确定用户的目标和需求，并归纳整理。\\n3. 列出需要查询的资料和天气、路况信息。\\n\\n## 输出格式：\\n- 列表形式，包含目标和需求、查询资料列表、天气和路况信息。\\n\\n## 限制：\\n- 不得提供未经过验证的信息。\\n- 所有数据需标注来源，不确定信息用[需核实]标记。\"},{\"role\":\"user\",\"content\":\"出发地：{{from}}\\n目的地：{{userQuestion}}\\n计划时间：{{time}}\\n人数：{{peopleNum}}\"}]},\"inputParams\":[{\"field\":\"content\",\"name\":\"userQuestion\",\"nodeId\":\"start-node\"},{\"field\":\"from\",\"name\":\"from\",\"nodeId\":\"start-node\"},{\"field\":\"time\",\"name\":\"time\",\"nodeId\":\"start-node\"},{\"field\":\"peopleNum\",\"name\":\"peopleNum\",\"nodeId\":\"start-node\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"251190209648521216\",\"type\":\"llm\",\"x\":943.2631578947365,\"y\":632.0526315789475,\"properties\":{\"text\":\"资料查询\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":30,\"messages\":[{\"role\":\"system\",\"content\":\"## 角色\\n\\n\\n你是一名 自驾游资料查询师（DataAgent），专注于为下游的“攻略生成Agent”提供精准、结构化的自驾游资料。\\n\\n\\n------\\n\\n\\n## 职责目标\\n 1. 根据输入内容（出发地、目的地、行程需求等），直接执行资料查询任务，不再向用户提问。\\n 2. 收集并整理以下四类信息：\\n  - 🚗 路线与导航规划信息\\n  - 🏞️ 沿途及目的地的景点和游玩项目\\n  - 🏨 住宿与周边美食信息\\n  - ☁️ 沿途及目的地天气信息\\n 3. 输出清晰、结构化的数据结果，供下一个Agent生成攻略使用。\\n\\n\\n------\\n\\n\\n## 能力与工具\\n\\n\\n- maps 工具\\n - 查询路线与导航规划信息（距离、时长、推荐路线、途经地）。\\n - 查询沿途及目的地的住宿与餐饮信息。\\n - 查询沿途及目的地的实时或近期天气信息。\\n\\n\\n- search 工具\\n - 查询沿途及目的地的景点、游玩项目、特色体验、门票及评价等。\\n\\n\\n------\\n\\n\\n## 工作流程\\n 1. 接收任务\\n  - 使用用户提供的现有信息（不提问、不二次确认）。\\n 2. 资料查询\\n  - 调用 maps 工具 获取路线、住宿、美食、天气。\\n  - 调用 search 工具 获取景点和游玩项目。\\n 3. 资料整理\\n  - 将查询结果按类型整理成结构化资料包。\\n  - 每条数据需注明来源（maps / search）。\\n 4. 结果输出\\n  - 输出格式清晰，便于下游Agent直接使用。\\n\\n\\n------\\n\\n\\n## 输出格式示例\\n\\n\\n```\\n{\\n  \\\"route_info\\\": [\\n    {\\n      \\\"from\\\": \\\"北京\\\",\\n      \\\"to\\\": \\\"张家口\\\",\\n      \\\"distance\\\": \\\"220km\\\",\\n      \\\"duration\\\": \\\"3小时\\\",\\n      \\\"route_detail\\\": \\\"经京藏高速G6\\\",\\n      \\\"source\\\": \\\"maps\\\"\\n    }\\n  ],\\n  \\\"sights\\\": [\\n    {\\n      \\\"name\\\": \\\"崇礼滑雪场\\\",\\n      \\\"tags\\\": [\\\"滑雪\\\", \\\"冬季运动\\\"],\\n      \\\"description\\\": \\\"亚洲知名滑雪胜地\\\",\\n      \\\"source\\\": \\\"search\\\"\\n    }\\n  ],\\n  \\\"hotels\\\": [\\n    {\\n      \\\"name\\\": \\\"张家口云顶假日酒店\\\",\\n      \\\"rating\\\": \\\"4.6\\\",\\n      \\\"address\\\": \\\"崇礼区奥运大道88号\\\",\\n      \\\"source\\\": \\\"maps\\\"\\n    }\\n  ],\\n  \\\"foods\\\": [\\n    {\\n      \\\"name\\\": \\\"张家口烧麦\\\",\\n      \\\"type\\\": \\\"地方特色\\\",\\n      \\\"recommendation\\\": \\\"崇礼老街美食街\\\",\\n      \\\"source\\\": \\\"maps\\\"\\n    }\\n  ],\\n  \\\"weather\\\": [\\n    {\\n      \\\"location\\\": \\\"崇礼\\\",\\n      \\\"condition\\\": \\\"晴\\\",\\n      \\\"temperature\\\": \\\"5°C~12°C\\\",\\n      \\\"wind\\\": \\\"微风\\\",\\n      \\\"source\\\": \\\"maps\\\"\\n    }\\n  ]\\n}\\n```\\n\\n\\n------\\n\\n\\n## 限制与规范\\n - 不生成行程攻略、总结或建议性文字。\\n - 不提问用户，只执行既定任务。\\n - 不包含任何虚构或未经验证的信息。\\n - 不涉及隐私、政治或违法内容。\\n - 不确定的数据需以 [需核实] 标识。\\n\\n\"},{\"role\":\"user\",\"content\":\"需求：{{demand}}\"}],\"plugins\":[{\"pluginId\":\"1983474860536475649\",\"pluginName\":\"高德\",\"category\":\"mcp\"},{\"pluginId\":\"1988091188723412994\",\"pluginName\":\"BraveSearch\",\"category\":\"mcp\"}]},\"inputParams\":[{\"field\":\"text\",\"name\":\"demand\",\"nodeId\":\"251170889153376256\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"251190922428542976\",\"type\":\"llm\",\"x\":1225.0526315789468,\"y\":370.6842105263157,\"properties\":{\"text\":\"生成文章\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":15,\"messages\":[{\"role\":\"system\",\"content\":\"# 角色定位：实地体验派自驾游攻略博主\\n\\n\\n你是一名热爱公路旅行、记录真实体验的自驾游达人博主。  \\n你的任务是为读者打造一份**能直接照着走的实地自驾游攻略**——兼顾实用性与可读性，让人看完就想出发。  \\n---\\n## 目标\\n1. 输出结构清晰、完整且可直接使用的自驾游攻略。  \\n2. 以**亲历者口吻**撰写内容，语言自然、有温度、具感染力。  \\n3. 帮助用户在有限时间内，完成一次轻松、安全、体验丰富的公路旅程。  \\n---\\n## 技能\\n1. **路线规划高手**：能结合季节、路况、天气等因素，规划最顺路、最合理的行程。  \\n2. **信息整合专家**：能整合住宿、美食、加油点、景点开放时间等要素，构成完整旅程。  \\n3. **避坑指导员**：能在攻略中明确提示潜在风险与替代路线，确保安全顺利出行。  \\n4. **文案风格创作者**：文风真实、有共鸣，兼具实用与情感温度。  \\n---\\n## 工作流程\\n1. **接收资料**\\n   - 使用上游 DataAgent 提供的结构化资料（路线、天气、景点、住宿、美食等）。  \\n   - 明确出发地点、目的地，时间和人数。\\n2. **内容整合**\\n   - 基于资料内容，编排合理的日程与路线逻辑。  \\n   - 为每天生成住宿与周边推荐。  \\n   - 根据沿途特点补充打卡点、体验亮点与避坑提醒。  \\n3. **配图搜索**\\n   - 基于文章中的景点，使用图片搜索工具搜索相关图片\\n   - 获取图片链接后，嵌入到文章中。\\n   - 每篇文章可以使用3张左右图片。\\n3. **攻略输出**\\n   - 输出格式固定、排版规范、语气一致、可直接用于图文发布。  \\n---\\n## 输出格式规范\\n\\n\\n\\n\\n攻略必须严格包含以下结构模块（顺序固定）：  \\n\\n\\n\\n\\n### 标题\\n一句话吸引读者，让人有“立刻出发”的冲动。  \\n> 示例：  \\n> 「这条西北环线美到窒息，一路风光大片连连！」  \\n---\\n### 重要概述信息（开篇摘要）\\n以简洁的段落或表格概述行程关键信息：  \\n- 出发地与目的地  \\n- 推荐出行季节  \\n- 建议行程天数  \\n- 总里程 / 主要路线  \\n- 车辆与路况建议  \\n- 是否适合家庭 / 情侣 / 越野爱好者  \\n\\n\\n\\n\\n> 示例：  \\n> **推荐季节**：9月下旬 - 10月中旬  \\n> **总里程**：约820公里  \\n> **适合人群**：喜欢自然风光与摄影的旅行者  \\n---\\n### 行程安排（按天）\\n分天描述路线、行驶距离、推荐出发时间、路况建议：  \\n- 每天路线与行驶信息  \\n- 沿途休息站 / 加油点  \\n- 建议游玩节奏  \\n\\n\\n\\n\\n> 示例：  \\n> **Day 1：成都 → 理县（约220km / 4小时）**  \\n> 上午出发，经成绵高速转都汶高速，全程路况优。途中可在汶川服务区短暂停留休息。  \\n---\\n### 每日住宿与周边推荐\\n为每天行程提供住宿推荐及周边美食娱乐选项：  \\n- 酒店名称、星级、亮点  \\n- 周边美食推荐（餐厅/夜宵/特色菜）  \\n- 休闲娱乐建议  \\n\\n\\n\\n\\n> 示例：  \\n> **住宿推荐**：理县瑞云山居（¥380起 / 含早餐）  \\n> **周边美食**：理县藏餐坊（推荐青稞酒与手抓羊）  \\n---\\n### 沿途打卡与景点推荐\\n精选每段路线的代表性景点与小众体验点，注明特色与亮点：  \\n> 示例：  \\n> - 毕棚沟：秋色摄影圣地，10月最佳观赏期  \\n> - 古尔沟温泉：天然碳酸泉，适合行程末放松  \\n---\\n### 避坑提醒\\n实地经验总结，包括但不限于：  \\n- 天气与季节风险  \\n- 路段注意事项（隧道、陡坡、限速）  \\n- 油站/信号盲区提示  \\n- 门票与政策更新  \\n\\n\\n> 示例：  \\n> - 国庆期间毕棚沟限流，建议提前预约。  \\n> - 高原路段昼夜温差大，请携带保暖衣物。  \\n---\\n### 结语\\n以温暖、真实的语气收尾，让读者感受到旅途的意义与期待。  \\n> 示例：  \\n> “这条路，值得你放慢脚步去感受。愿每一次出发，都有风景，也有故事。”  \\n---\\n## 风格要求\\n- 文字自然、口语化、有画面感。  \\n- 语气积极向上，不生硬、不堆砌。  \\n- 以**“亲身体验分享”**为写作视角。  \\n- 适合直接发布到公众号 / 小红书 / 旅游类平台。  \\n---\\n## 限制与合规说明\\n- 不虚构信息，所有数据基于实际资料。  \\n- 不涉及隐私、歧视或违法内容。  \\n- 直接输出攻略正文，不输出系统提示、元信息或额外解释。  \\n\\n\"},{\"role\":\"user\",\"content\":\"## 需求\\n{{demand}}\\n## 资料\\n{{resources}}\"}],\"plugins\":[{\"pluginId\":\"1988208474780168193\",\"pluginName\":\"图片搜索\",\"category\":\"mcp\"}]},\"inputParams\":[{\"field\":\"text\",\"name\":\"demand\",\"nodeId\":\"251170889153376256\"},{\"field\":\"text\",\"name\":\"resources\",\"nodeId\":\"251190209648521216\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}},{\"id\":\"251191126401740800\",\"type\":\"end\",\"x\":1743.594965675057,\"y\":370.28146453089266,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"{{result}}\"},\"inputParams\":[],\"outputParams\":[{\"field\":\"text\",\"name\":\"result\",\"nodeId\":\"251246385341919232\"}],\"height\":136,\"width\":332}},{\"id\":\"251246385341919232\",\"type\":\"llm\",\"x\":1438.6270022883289,\"y\":637.045766590389,\"properties\":{\"text\":\"润色并存储\",\"options\":{\"model\":{\"modeId\":\"1890232564262739969\",\"params\":{\"model\":\"OpenAI\",\"temperature\":0.7}},\"history\":10,\"messages\":[{\"role\":\"system\",\"content\":\"# 角色  \\n你是一个**出行攻略与富文本排版专家（TravelContentStylist）**，负责将普通文本文章转化为专业级、高端杂志风格的HTML富文本内容。\\n\\n\\n---\\n\\n\\n## 🎯 目标  \\n1. 将输入的原始文章格式化为整洁、美观、结构统一的HTML富文本（视觉风格参考蓝灰色出行攻略模板）。  \\n2. 自动提取文章的标题、副标题、关键词、摘要等信息。  \\n3. 将富文本与提取信息一并存储到系统中。  \\n\\n\\n---\\n\\n\\n## 💡 富文本设计规范  \\n\\n\\n### 页面整体样式示意 \\n```html\\n<article style=\\\"font-family: \'PingFang SC\', \'Microsoft YaHei\', Arial, sans-serif;\\n                color: #333; background-color: #FAFBFC; \\n                max-width: 880px; margin: 40px auto; padding: 40px; \\n                border-radius: 12px; box-shadow: 0 0 12px rgba(0,0,0,0.05);\\\">\\n\\n\\n\\n\\n  <header style=\\\"border-bottom: 2px solid #E6EAF0; margin-bottom: 24px;\\\">\\n    <h1 style=\\\"font-size: 2em; font-weight: 700; color: #1A73E8; margin-bottom: 6px;\\\">文章主标题</h1>\\n    <h2 style=\\\"font-size: 1.2em; font-weight: 400; color: #555;\\\">文章副标题</h2>\\n  </header>\\n\\n\\n\\n\\n  <!-- 信息概览模块 -->\\n  <section style=\\\"background: #F3F7FC; border-left: 4px solid #1A73E8; \\n                  padding: 16px 20px; border-radius: 8px; margin-bottom: 28px;\\\">\\n    <p><strong>出行季节：</strong>金秋10月（2023-10-25）</p>\\n    <p><strong>推荐天数：</strong>1-2天（可夜宿济南/南京）</p>\\n    <p><strong>总公里数：</strong>约1200km</p>\\n    <p><strong>人数与车型建议：</strong>5人，推荐中大型SUV或MPV，空间舒适。</p>\\n  </section>\\n  <!-- 其他部分 -->\\n</article>\\n```  \\n设计说明\\n1. 整体布局\\n  - 最大宽度约 880px，居中显示，整体背景为浅灰白色（#FAFBFC）。\\n  - 内边距较大（40px），四周有圆角（12px）和轻微阴影（box-shadow: 0 0 12px rgba(0,0,0,0.05)），营造卡片式感觉。\\n  - 使用了中文常用的字体组合（苹方、微软雅黑），兼顾现代感与易读性，文字颜色为深灰色（#333）。\\n2. 标题部分\\n  - 主标题突出（2em，蓝色 #1A73E8，粗体），副标题较小（1.2em，深灰色），并在底部有分隔线强化层次感。\\n3. 信息概览模块\\n  - 背景为淡蓝色（#F3F7FC），左侧有蓝色竖条（4px），像标签式信息卡。\\n  - 内部列出了出行季节、推荐天数、总公里数和人数/车型建议，文字加粗强调关键信息。\\n  - 模块与下方内容有明显间距（28px），便于视觉区分。\\n4. 行程安排模块\\n  - 模块标题蓝色，带下划分隔线，列表为有序列表，行距 1.8，便于阅读行程顺序。\\n  - 每天的路线、里程和时间都清晰标注，关键内容加粗突出。\\n5. 沿途打卡模块\\n  - 模块标题与行程安排相同风格，列表为无序列表，展示沿途景点和推荐打卡地。\\n  - 行距同样较大（1.8），保持阅读舒适度。\\n6. 注意事项模块（避坑提醒）\\n  - 模块标题用红色（#D93025）和粉色分隔线（#F3C1BE），突出警示性质。\\n  - 列表中重点信息加粗（施工提醒、天气因素、油费与通行），提醒用户注意行程安全和预算。\\n整体风格 清爽、层次分明、信息易抓取，既有蓝色调的出行信息模块，又有红色警示提醒，结合圆角卡片和阴影设计，使文章既专业又具有亲和力。\\n\\n\\n3. 提取标题、副标题、关键词、摘要并生成结构化信息：\\n\\n\\n{\\n  \\\"title\\\": \\\"文章主标题\\\",\\n  \\\"subtitle\\\": \\\"文章副标题\\\",\\n  \\\"keywords\\\": [\\\"关键词1\\\", \\\"关键词2\\\", \\\"关键词3\\\"],\\n  \\\"summary\\\": \\\"简要描述文章主题与亮点。\\\"\\n}\\n4. 将HTML富文本与提取信息存入系统。\\n5. 返回执行结果（不输出HTML内容本身）：\\n{\\n  \\\"status\\\": \\\"success\\\",\\n  \\\"message\\\": \\\"富文本文章已成功优化并入库。\\\"\\n}\\n\\n\\n\\n\\n⸻\\n\\n\\n\\n\\n## 输出要求\\n- 不输出HTML正文，只返回操作结果。\\n- 富文本需使用 <p> 包裹正文段落，整体结构采用 <article> 容器。\\n- 排版需超过一般模板美感（具备视觉层次、柔和色彩与可印刷风格）。\\n- 输出格式严格为JSON结果对象，保证系统可解析与存储。\\n- 注意原文内容不要丢失，特别是配图等信息\\n\\n\\n\\n\\n\\n\"},{\"role\":\"user\",\"content\":\"{{content}}\"}],\"plugins\":[{\"pluginId\":\"1988146198605819905\",\"pluginName\":\"JeecgBoot CMS\",\"category\":\"mcp\"}]},\"inputParams\":[{\"field\":\"text\",\"name\":\"content\",\"nodeId\":\"251190922428542976\"}],\"outputParams\":[{\"field\":\"text\",\"name\":\"回复内容\",\"type\":\"string\"}],\"height\":180,\"width\":332}}],\"edges\":[{\"id\":\"251170889157570560\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"251170889153376256\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"251170889153376256_input\",\"pointsList\":[{\"x\":498.8947368421053,\"y\":574.0526315789475},{\"x\":598.8947368421053,\"y\":574.0526315789475},{\"x\":419.9473684210526,\"y\":322.05263157894734},{\"x\":519.9473684210526,\"y\":322.05263157894734}]},{\"id\":\"251190922432737280\",\"type\":\"base-edge\",\"sourceNodeId\":\"251190209648521216\",\"targetNodeId\":\"251190922428542976\",\"sourceAnchorId\":\"251190209648521216_output\",\"targetAnchorId\":\"251190922428542976_input\",\"pointsList\":[{\"x\":1109.2631578947367,\"y\":573.0526315789475},{\"x\":1209.2631578947367,\"y\":573.0526315789475},{\"x\":959.0526315789468,\"y\":311.6842105263157},{\"x\":1059.0526315789468,\"y\":311.6842105263157}]},{\"id\":\"251221680912330752\",\"type\":\"base-edge\",\"sourceNodeId\":\"251170889153376256\",\"targetNodeId\":\"251190209648521216\",\"sourceAnchorId\":\"251170889153376256_output\",\"targetAnchorId\":\"251190209648521216_input\",\"pointsList\":[{\"x\":851.9473684210526,\"y\":322.05263157894734},{\"x\":951.9473684210526,\"y\":322.05263157894734},{\"x\":677.2631578947365,\"y\":573.0526315789475},{\"x\":777.2631578947365,\"y\":573.0526315789475}]},{\"id\":\"251246385346113536\",\"type\":\"base-edge\",\"sourceNodeId\":\"251190922428542976\",\"targetNodeId\":\"251246385341919232\",\"sourceAnchorId\":\"251190922428542976_output\",\"targetAnchorId\":\"251246385341919232_input\",\"pointsList\":[{\"x\":1391.0526315789468,\"y\":311.6842105263157},{\"x\":1491.0526315789468,\"y\":311.6842105263157},{\"x\":1172.6270022883289,\"y\":578.045766590389},{\"x\":1272.6270022883289,\"y\":578.045766590389}]},{\"id\":\"251246471618752512\",\"type\":\"base-edge\",\"sourceNodeId\":\"251246385341919232\",\"targetNodeId\":\"251191126401740800\",\"sourceAnchorId\":\"251246385341919232_output\",\"targetAnchorId\":\"251191126401740800_input\",\"pointsList\":[{\"x\":1604.6270022883289,\"y\":578.045766590389},{\"x\":1704.6270022883289,\"y\":578.045766590389},{\"x\":1477.594965675057,\"y\":333.28146453089266},{\"x\":1577.594965675057,\"y\":333.28146453089266}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"目的地\",\"required\":true,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"},{\"field\":\"from\",\"name\":\"出发地\",\"required\":true,\"type\":\"string\"},{\"field\":\"time\",\"name\":\"出发时间\",\"required\":true,\"type\":\"string\"},{\"field\":\"peopleNum\",\"name\":\"人数\",\"required\":true,\"type\":\"number\"}]}');
INSERT INTO `airag_flow` VALUES ('2021912569829470210', 'admin', '2026-02-12 19:41:37', 'admin', '2026-02-12 19:43:24', NULL, NULL, 'jeecg', '提示词', '', 'temp/v2-cec26f736cc5349669c51b220360ad20_r_1770896281174.png', 'THEN(\n    start.tag(\'start-node\'),\n    reply.tag(\'285012645993926656\'),\n    end.tag(\'285012708937846784\')\n).tag(\"start-node\")', '{\"nodes\":[{\"id\":\"start-node\",\"type\":\"start\",\"x\":300,\"y\":437,\"properties\":{\"text\":\"开始\",\"remarks\":\"\",\"options\":{},\"inputParams\":[{\"field\":\"content\",\"name\":\"用户问题\",\"type\":\"string\",\"required\":false},{\"field\":\"history\",\"name\":\"历史记录\",\"type\":\"string[]\",\"required\":false}],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"285012645993926656\",\"type\":\"reply\",\"x\":786,\"y\":437,\"properties\":{\"text\":\"直接回复\",\"options\":{\"content\":\"我不好\",\"stream\":false},\"inputParams\":[],\"outputParams\":[],\"height\":92,\"width\":332}},{\"id\":\"285012708937846784\",\"type\":\"end\",\"x\":1272,\"y\":448,\"properties\":{\"text\":\"结束\",\"options\":{\"outputText\":true,\"outputContent\":\"你好\"},\"inputParams\":[],\"outputParams\":[],\"height\":114,\"width\":332}}],\"edges\":[{\"id\":\"285012645998120960\",\"type\":\"base-edge\",\"sourceNodeId\":\"start-node\",\"targetNodeId\":\"285012645993926656\",\"sourceAnchorId\":\"start-node_output\",\"targetAnchorId\":\"285012645993926656_input\",\"pointsList\":[{\"x\":466,\"y\":422},{\"x\":566,\"y\":422},{\"x\":520,\"y\":422},{\"x\":620,\"y\":422}]},{\"id\":\"285012708942041088\",\"type\":\"base-edge\",\"sourceNodeId\":\"285012645993926656\",\"targetNodeId\":\"285012708937846784\",\"sourceAnchorId\":\"285012645993926656_output\",\"targetAnchorId\":\"285012708937846784_input\",\"pointsList\":[{\"x\":952,\"y\":422},{\"x\":1052,\"y\":422},{\"x\":1006,\"y\":422},{\"x\":1106,\"y\":422}]}]}', 'enable', '{\"outputs\":[{\"field\":\"outputText\",\"type\":\"string\"}],\"inputs\":[{\"field\":\"content\",\"name\":\"用户问题\",\"required\":false,\"type\":\"string\"},{\"field\":\"history\",\"name\":\"历史记录\",\"required\":false,\"type\":\"string[]\"}]}');

-- ----------------------------
-- Table structure for airag_knowledge
-- ----------------------------
DROP TABLE IF EXISTS `airag_knowledge`;
CREATE TABLE `airag_knowledge`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '租户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '知识库名称',
  `descr` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `embed_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '向量模型id',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of airag_knowledge
-- ----------------------------
INSERT INTO `airag_knowledge` VALUES ('2008478334721929217', 'admin', '2026-01-06 17:58:45', 'admin', '2026-02-10 21:50:38', 'A01A03', NULL, '通义知识库', '通义知识库', '2014260401715068930', 'enable');

-- ----------------------------
-- Table structure for airag_knowledge_doc
-- ----------------------------
DROP TABLE IF EXISTS `airag_knowledge_doc`;
CREATE TABLE `airag_knowledge_doc`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '租户id',
  `knowledge_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '知识库id',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标题',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '类型',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态',
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '元数据',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of airag_knowledge_doc
-- ----------------------------

-- ----------------------------
-- Table structure for airag_mcp
-- ----------------------------
DROP TABLE IF EXISTS `airag_mcp`;
CREATE TABLE `airag_mcp`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图标',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
  `descr` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `category` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'mcp' COMMENT '类型（plugin=插件，mcp=MCP）',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'mcp类型（sse：sse类型；stdio：标准类型）',
  `endpoint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '服务端点（SSE类型为URL，stdio类型为命令）',
  `headers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '请求头（sse类型）、环境变量（stdio类型）',
  `tools` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '工具列表',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '状态（enable=启用、disable=禁用）',
  `synced` int NULL DEFAULT NULL COMMENT '是否同步',
  `metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '元数据',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI MCP' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of airag_mcp
-- ----------------------------
INSERT INTO `airag_mcp` VALUES ('1983474860536475649', NULL, '高德MCP', '高德MCP，包含查询路线、路况、天气等工具', 'mcp', 'sse', 'https://mcp.amap.com/sse?key=您在高德官网上申请的key', '', '[{\"name\":\"maps_direction_bicycling\",\"description\":\"骑行路径规划用于规划骑行通勤方案，规划时会考虑天桥、单行线、封路等情况。最大支持 500km 的骑行路线规划\",\"parameters\":[{\"name\":\"origin\",\"description\":\"出发点经纬度，坐标格式为：经度，纬度\",\"required\":true},{\"name\":\"destination\",\"description\":\"目的地经纬度，坐标格式为：经度，纬度\",\"required\":true}]},{\"name\":\"maps_direction_driving\",\"description\":\"驾车路径规划 API 可以根据用户起终点经纬度坐标规划以小客车、轿车通勤出行的方案，并且返回通勤方案的数据。\",\"parameters\":[{\"name\":\"origin\",\"description\":\"出发点经纬度，坐标格式为：经度，纬度\",\"required\":true},{\"name\":\"destination\",\"description\":\"目的地经纬度，坐标格式为：经度，纬度\",\"required\":true}]},{\"name\":\"maps_direction_transit_integrated\",\"description\":\"根据用户起终点经纬度坐标规划综合各类公共（火车、公交、地铁）交通方式的通勤方案，并且返回通勤方案的数据，跨城场景下必须传起点城市与终点城市\",\"parameters\":[{\"name\":\"origin\",\"description\":\"出发点经纬度，坐标格式为：经度，纬度\",\"required\":true},{\"name\":\"destination\",\"description\":\"目的地经纬度，坐标格式为：经度，纬度\",\"required\":true},{\"name\":\"city\",\"description\":\"公共交通规划起点城市\",\"required\":true},{\"name\":\"cityd\",\"description\":\"公共交通规划终点城市\",\"required\":true}]},{\"name\":\"maps_direction_walking\",\"description\":\"根据输入起点终点经纬度坐标规划100km 以内的步行通勤方案，并且返回通勤方案的数据\",\"parameters\":[{\"name\":\"origin\",\"description\":\"出发点经度，纬度，坐标格式为：经度，纬度\",\"required\":true},{\"name\":\"destination\",\"description\":\"目的地经度，纬度，坐标格式为：经度，纬度\",\"required\":true}]},{\"name\":\"maps_distance\",\"description\":\"测量两个经纬度坐标之间的距离,支持驾车、步行以及球面距离测量\",\"parameters\":[{\"name\":\"origins\",\"description\":\"起点经度，纬度，可以传多个坐标，使用竖线隔离，比如120,30|120,31，坐标格式为：经度，纬度\",\"required\":true},{\"name\":\"destination\",\"description\":\"终点经度，纬度，坐标格式为：经度，纬度\",\"required\":true},{\"name\":\"type\",\"description\":\"距离测量类型,1代表驾车距离测量，0代表直线距离测量，3步行距离测量\"}]},{\"name\":\"maps_geo\",\"description\":\"将详细的结构化地址转换为经纬度坐标。支持对地标性名胜景区、建筑物名称解析为经纬度坐标\",\"parameters\":[{\"name\":\"address\",\"description\":\"待解析的结构化地址信息\",\"required\":true},{\"name\":\"city\",\"description\":\"指定查询的城市\"}]},{\"name\":\"maps_regeocode\",\"description\":\"将一个高德经纬度坐标转换为行政区划地址信息\",\"parameters\":[{\"name\":\"location\",\"description\":\"经纬度\",\"required\":true}]},{\"name\":\"maps_ip_location\",\"description\":\"IP 定位根据用户输入的 IP 地址，定位 IP 的所在位置\",\"parameters\":[{\"name\":\"ip\",\"description\":\"IP地址\",\"required\":true}]},{\"name\":\"maps_schema_personal_map\",\"description\":\"用于行程规划结果在高德地图展示。将行程规划位置点按照行程顺序填入lineList，返回结果为高德地图打开的URI链接，该结果不需总结，直接返回！\",\"parameters\":[{\"name\":\"orgName\",\"description\":\"行程规划地图小程序名称\",\"required\":true},{\"name\":\"lineList\",\"description\":\"行程列表\",\"required\":true}]},{\"name\":\"maps_around_search\",\"description\":\"周边搜，根据用户传入关键词以及坐标location，搜索出radius半径范围的POI\",\"parameters\":[{\"name\":\"keywords\",\"description\":\"搜索关键词\",\"required\":true},{\"name\":\"location\",\"description\":\"中心点经度纬度\",\"required\":true},{\"name\":\"radius\",\"description\":\"搜索半径\"}]},{\"name\":\"maps_search_detail\",\"description\":\"查询关键词搜或者周边搜获取到的POI ID的详细信息\",\"parameters\":[{\"name\":\"id\",\"description\":\"关键词搜或者周边搜获取到的POI ID\",\"required\":true}]},{\"name\":\"maps_text_search\",\"description\":\"关键字搜索 API 根据用户输入的关键字进行 POI 搜索，并返回相关的信息\",\"parameters\":[{\"name\":\"keywords\",\"description\":\"查询关键字\",\"required\":true},{\"name\":\"city\",\"description\":\"查询城市\"},{\"name\":\"citylimit\",\"description\":\"是否限制城市范围内搜索，默认不限制\"}]},{\"name\":\"maps_schema_navi\",\"description\":\" Schema唤醒客户端-导航页面，用于根据用户输入终点信息，返回一个拼装好的客户端唤醒URI，用户点击该URI即可唤起对应的客户端APP。唤起客户端后，会自动跳转到导航页面。\",\"parameters\":[{\"name\":\"lon\",\"description\":\"终点经度\",\"required\":true},{\"name\":\"lat\",\"description\":\"终点纬度\",\"required\":true}]},{\"name\":\"maps_schema_take_taxi\",\"description\":\"根据用户输入的起点和终点信息，返回一个拼装好的客户端唤醒URI，直接唤起高德地图进行打车。直接展示生成的链接，不需要总结\",\"parameters\":[{\"name\":\"slon\",\"description\":\"起点经度\"},{\"name\":\"slat\",\"description\":\"起点纬度\"},{\"name\":\"sname\",\"description\":\"起点名称\"},{\"name\":\"dlon\",\"description\":\"终点经度\",\"required\":true},{\"name\":\"dlat\",\"description\":\"终点纬度\",\"required\":true},{\"name\":\"dname\",\"description\":\"终点名称\",\"required\":true}]},{\"name\":\"maps_weather\",\"description\":\"根据城市名称或者标准adcode查询指定城市的天气\",\"parameters\":[{\"name\":\"city\",\"description\":\"城市名称或者adcode\",\"required\":true}]}]', 'enable', 1, '{\"tool_count\":15}', 'admin', '2025-10-29 18:03:53', 'admin', '2025-11-27 18:18:16', 'A04', NULL);
INSERT INTO `airag_mcp` VALUES ('1986312214909321217', NULL, '商品采购助手', '为AI Agent提供商品管理API（DEMO)', 'plugin', 'api', NULL, '', '[{\"name\":\"list_products\",\"description\":\"查询可购买的商品列表,支持按分类和关键词筛选；商品的id只能通过这个接口查询。\",\"path\":\"/demo/shop/products\",\"method\":\"GET\",\"enabled\":true,\"parameters\":[{\"name\":\"category\",\"description\":\"商品分类,可选值: \\\"电子产品\\\", \\\"图书\\\", \\\"生活用品\\\", \\\"食品\\\"\",\"type\":\"String\",\"location\":\"Query\",\"required\":false,\"defaultValue\":\"\"},{\"name\":\"keyword\",\"description\":\"搜索关键词,用于在商品名称和描述中搜索\",\"type\":\"String\",\"location\":\"Query\",\"required\":false,\"defaultValue\":\"\"}],\"responses\":[{\"name\":\"id\",\"description\":\"商品ID，对应下单的ProductId\",\"type\":\"String\"},{\"name\":\"name\",\"description\":\"商品名称\",\"type\":\"String\"},{\"name\":\"price\",\"description\":\"价格(元)\",\"type\":\"String\"},{\"name\":\"category\",\"description\":\"分类\",\"type\":\"String\"},{\"name\":\"description\",\"description\":\"描述\",\"type\":\"String\"},{\"name\":\"stock\",\"description\":\"库存数量\",\"type\":\"String\"}]},{\"name\":\"check_stock\",\"description\":\"查询指定商品的当前库存情况\",\"path\":\"/demo/shop/stock\",\"method\":\"GET\",\"enabled\":true,\"parameters\":[{\"name\":\"productId\",\"description\":\"商品ID\",\"type\":\"String\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"\"}],\"responses\":[{\"name\":\"productId\",\"description\":\"商品ID\",\"type\":\"String\"},{\"name\":\"productName\",\"description\":\"商品名称\",\"type\":\"String\"},{\"name\":\"stock\",\"description\":\"当前库存数量\",\"type\":\"String\"},{\"name\":\"available\",\"description\":\"是否有货\",\"type\":\"Boolean\"}]},{\"name\":\"create_order\",\"description\":\"为用户创建购买订单\",\"path\":\"/demo/shop/purchase\",\"method\":\"POST\",\"enabled\":true,\"parameters\":[{\"name\":\"productId\",\"description\":\"要购买的商品的ID，对应商品的信息的id\",\"type\":\"String\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"\"},{\"name\":\"quantity\",\"description\":\" 购买数量,必须大于0\",\"type\":\"Integer\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"\"},{\"name\":\"userId\",\"description\":\"用户ID,用于关联订单\",\"type\":\"String\",\"location\":\"Query\",\"required\":false,\"defaultValue\":\"\"}],\"responses\":[{\"name\":\"id\",\"description\":\"订单ID\",\"type\":\"String\"},{\"name\":\"productId\",\"description\":\"商品ID\",\"type\":\"String\"},{\"name\":\"productName\",\"description\":\"商品名称\",\"type\":\"String\"},{\"name\":\"quantity\",\"description\":\"购买数量\",\"type\":\"String\"},{\"name\":\"unitPrice\",\"description\":\"单价\",\"type\":\"String\"},{\"name\":\"totalAmount\",\"description\":\"总金额\",\"type\":\"String\"},{\"name\":\"status\",\"description\":\"订单状态\",\"type\":\"String\"},{\"name\":\"createTime\",\"description\":\"创建时间\",\"type\":\"String\"}]},{\"name\":\"confirm_payment\",\"description\":\"确认订单支付,扣减商品库存\",\"path\":\"/demo/shop/stock/deduct\",\"method\":\"POST\",\"enabled\":true,\"parameters\":[{\"name\":\"orderId\",\"description\":\"订单ID\",\"type\":\"String\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"\"}],\"responses\":[{\"name\":\"orderId\",\"description\":\"订单ID\",\"type\":\"String\"},{\"name\":\"productId\",\"description\":\"商品ID\",\"type\":\"String\"},{\"name\":\"productName\",\"description\":\"商品名称\",\"type\":\"String\"},{\"name\":\"deductedQuantity\",\"description\":\"扣减的数量\",\"type\":\"String\"},{\"name\":\"remainingStock\",\"description\":\"剩余库存\",\"type\":\"String\"},{\"name\":\"orderStatus\",\"description\":\"订单状态\",\"type\":\"String\"}]},{\"name\":\"get_order_details\",\"description\":\"查询指定订单的详细信息\",\"path\":\"/demo/shop/order\",\"method\":\"GET\",\"enabled\":true,\"parameters\":[{\"name\":\"orderId\",\"description\":\"订单ID\",\"type\":\"String\",\"location\":\"Query\",\"required\":false,\"defaultValue\":\"\"}],\"responses\":[]},{\"name\":\"get_categories\",\"description\":\"获取所有可用的商品分类\",\"path\":\"/demo/shop/categories\",\"method\":\"GET\",\"enabled\":true,\"parameters\":[],\"responses\":[]}]', 'enable', 1, '{\"tokenParamName\":\"X-Access-Token\",\"tool_count\":6,\"authType\":\"token\",\"tokenParamValue\":\"\"}', 'admin', '2025-11-06 13:58:31', 'admin', '2025-11-13 17:16:57', 'A05A01A01', NULL);
INSERT INTO `airag_mcp` VALUES ('1988091188723412994', NULL, 'BraveSearch', '基于Brave的网络检索插件，支持使用Brave搜索资料', 'plugin', 'api', 'https://api.search.brave.com', '{\"Accept\":\"*/*\",\"X-Subscription-Token\":\"???\"}', '[{\"name\":\"search\",\"description\":\"从搜索引擎根据问题搜索结果\",\"path\":\"/res/v1/web/search\",\"method\":\"GET\",\"enabled\":true,\"parameters\":[{\"name\":\"count\",\"description\":\"查询数量\",\"type\":\"String\",\"location\":\"Query\",\"required\":false,\"defaultValue\":\"10\"},{\"name\":\"q\",\"description\":\"查询内容（问题）\",\"type\":\"String\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"\"}],\"responses\":[{\"name\":\"title\",\"description\":\"结果标题\",\"type\":\"String\"},{\"name\":\"description\",\"description\":\"结果描述\",\"type\":\"String\"},{\"name\":\"url\",\"description\":\"结果原文地址\",\"type\":\"String\"}]}]', 'enable', 1, '{\"tokenParamName\":\"X-Subscription-Token\",\"tool_count\":1,\"authType\":\"token\",\"tokenParamValue\":\"BSATNKM5e6Hm_2LewptVvLSn0eDzWf6\"}', 'admin', '2025-11-11 11:47:31', 'admin', '2025-11-13 17:16:52', 'A05A01A01', NULL);
INSERT INTO `airag_mcp` VALUES ('1988208474780168193', NULL, 'Unsplash', '图片搜索插件，支持用关键词搜索相关图片', 'plugin', 'api', 'https://api.unsplash.com', '{\"Accept-Version\":\"v1\",\"Authorization\":\"Client-ID ???\"}', '[{\"name\":\"search_photos\",\"description\":\"通过接口查询与关键词相关的图片列表。\",\"path\":\"/search/photos\",\"method\":\"GET\",\"enabled\":true,\"parameters\":[{\"name\":\"page\",\"description\":\"分页页码，数字类型\",\"type\":\"Number\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"1\"},{\"name\":\"per_page\",\"description\":\"每页数量\",\"type\":\"Number\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"1\"},{\"name\":\"query\",\"description\":\"关键词，对图片的描述（查询条件）\",\"type\":\"String\",\"location\":\"Query\",\"required\":true,\"defaultValue\":\"\"}],\"responses\":[{\"name\":\"results\",\"description\":\"查询到的结果集合\",\"type\":\"Array\"},{\"name\":\"urls\",\"description\":\"结果集合中的图片地址，根据清晰度有多个选项\",\"type\":\"Array\"}]}]', 'enable', 1, '{\"tokenParamName\":\"Authorization\",\"tool_count\":1,\"authType\":\"token\",\"tokenParamValue\":\"Client-ID Ixug6rX2j1PMb08A0HRpwny8dAWi1vBLN1gymow75LQ\"}', 'admin', '2025-11-11 19:33:34', 'admin', '2025-11-13 17:16:45', 'A05A01A01', NULL);

-- ----------------------------
-- Table structure for airag_model
-- ----------------------------
DROP TABLE IF EXISTS `airag_model`;
CREATE TABLE `airag_model`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属部门',
  `tenant_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '租户id',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '名称',
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '供应者',
  `model_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型名称',
  `credential` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '凭证信息',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'API域名',
  `model_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型类型',
  `model_params` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '模型参数',
  `activate_flag` int NULL DEFAULT NULL COMMENT '是否激活（1=是，0=否）',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of airag_model
-- ----------------------------
INSERT INTO `airag_model` VALUES ('1890232564262739969', 'jeecg', '2025-02-14 10:52:16', 'admin', '2025-11-27 18:18:33', 'A04', NULL, 'OpenAI', 'OPENAI', 'gpt-4o-mini', '{\"apiKey\":\"您在OpenAPI官网上申请的key\"}', 'https://api.gpt.ge', 'LLM', '{\"temperature\":0.2,\"topP\":0.7,\"presencePenalty\":0.5,\"frequencyPenalty\":0.5,\"maxTokens\":null}', 1);
INSERT INTO `airag_model` VALUES ('1891459707122499586', 'jeecg', '2025-02-17 20:08:30', 'admin', '2025-11-27 18:18:38', 'A04', NULL, 'OpenAI向量', 'OPENAI', 'text-embedding-ada-002', '{\"apiKey\":\"您在OpenAPI官网上申请的key\"}', 'https://api.gpt.ge', 'EMBED', NULL, 1);
INSERT INTO `airag_model` VALUES ('1897481367743143938', 'jeecg', '2025-03-06 10:56:26', 'admin', '2025-11-27 18:19:06', 'A04', NULL, 'deepseek', 'DEEPSEEK', 'deepseek-chat', '{\"apiKey\":\"您在官网上申请的key\"}', 'https://api.deepseek.com/v1', 'LLM', NULL, 0);
INSERT INTO `airag_model` VALUES ('1897883052995006466', 'jeecg', '2025-03-07 13:32:35', 'admin', '2025-11-27 18:18:55', 'A04', NULL, '智谱', 'ZHIPU', 'glm-4-flash', '{\"apiKey\":\"您在官网上申请的key\"}', 'https://open.bigmodel.cn/', 'LLM', NULL, 0);
INSERT INTO `airag_model` VALUES ('1897884353107611650', 'jeecg', '2025-03-07 13:37:45', 'admin', '2025-11-27 18:19:00', 'A04', NULL, '智谱向量', 'ZHIPU', 'Embedding-3', '{\"apiKey\":\"您在官网上申请的key\"}', 'https://open.bigmodel.cn', 'EMBED', '{\"temperature\":0.7,\"topP\":0.7,\"presencePenalty\":null,\"frequencyPenalty\":null,\"maxTokens\":null}', 0);
INSERT INTO `airag_model` VALUES ('2014260361990815746', 'admin', '2026-01-22 16:54:28', NULL, NULL, NULL, NULL, '通义千问', 'QWEN', 'qwen3-omni-flash-2025-12-01', '{\"apiKey\":\"您在官网上申请的key\"}', 'https://dashscope.aliyuncs.com/api/v1/services/', 'LLM', NULL, 1);
INSERT INTO `airag_model` VALUES ('2014260401715068930', 'admin', '2026-01-22 16:54:38', 'admin', '2026-02-12 19:15:41', NULL, NULL, '通义千问', 'QWEN', 'text-embedding-v2', '{\"apiKey\":\"您在官网上申请的key\"}', 'https://dashscope.aliyuncs.com/api/v1/services/', 'EMBED', NULL, 1);

-- ----------------------------
-- Table structure for cs_agent
-- ----------------------------
DROP TABLE IF EXISTS `cs_agent`;
CREATE TABLE `cs_agent`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联系统用户ID',
  `nickname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客服昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `max_sessions` int NULL DEFAULT 10 COMMENT '最大同时接待数',
  `welcome_message` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '欢迎语',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0-离线 1-在线 2-忙碌',
  `current_sessions` int NULL DEFAULT 0 COMMENT '当前接待数',
  `total_served` int NULL DEFAULT 0 COMMENT '累计服务数',
  `default_app_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '默认AI应用ID',
  `last_online_time` datetime NULL DEFAULT NULL COMMENT '最后在线时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `role` int NULL DEFAULT 0 COMMENT '角色: 0-普通客服 1-管理者(可监控所有会话)',
  `parent_agent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父客服ID(管理员客服创建的子客服)',
  `allowed_menus` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '可见菜单列表(JSON数组)',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_agent
-- ----------------------------

-- ----------------------------
-- Table structure for cs_agent_ip_whitelist
-- ----------------------------
DROP TABLE IF EXISTS `cs_agent_ip_whitelist`;
CREATE TABLE `cs_agent_ip_whitelist`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'IP或CIDR段',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服IP白名单' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_agent_ip_whitelist
-- ----------------------------

-- ----------------------------
-- Table structure for cs_agent_login_log
-- ----------------------------
DROP TABLE IF EXISTS `cs_agent_login_log`;
CREATE TABLE `cs_agent_login_log`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `login_date` datetime NULL DEFAULT NULL COMMENT '日期',
  `username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '账号',
  `event` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '事件',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'IP地址',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_login_date`(`login_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服登录日志' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_agent_login_log
-- ----------------------------

-- ----------------------------
-- Table structure for cs_brand_config
-- ----------------------------
DROP TABLE IF EXISTS `cs_brand_config`;
CREATE TABLE `cs_brand_config`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `app_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '系统名称',
  `app_short_title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '系统简称',
  `app_subtitle` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录页副标题',
  `logo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'Logo地址',
  `favicon_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器图标',
  `login_bg_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '登录页背景图',
  `loading_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '加载页文案',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态(1启用0禁用)',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除标记',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服系统品牌配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_brand_config
-- ----------------------------
INSERT INTO `cs_brand_config` VALUES ('dea5b3c7f5ce11f089be56ca9804d785', '客服系统', '客服系统', '欢迎使用客服系统', '/logo.svg', '/logo.svg', '', '客服系统', 1, 0, 'admin', '2026-01-20 15:08:49', 'admin', '2026-02-28 15:00:35');

-- ----------------------------
-- Table structure for cs_collaborator
-- ----------------------------
DROP TABLE IF EXISTS `cs_collaborator`;
CREATE TABLE `cs_collaborator`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `conversation_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `agent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客服ID',
  `role` tinyint NULL DEFAULT 1 COMMENT '角色: 0-主负责 1-协作者 2-临时介入',
  `join_time` datetime NULL DEFAULT NULL COMMENT '加入时间',
  `leave_time` datetime NULL DEFAULT NULL COMMENT '离开时间',
  `invite_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邀请人ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_conv_agent`(`conversation_id` ASC, `agent_id` ASC) USING BTREE,
  INDEX `idx_agent`(`agent_id` ASC) USING BTREE,
  INDEX `idx_conversation`(`conversation_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会话协作者表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_collaborator
-- ----------------------------

-- ----------------------------
-- Table structure for cs_conversation
-- ----------------------------
DROP TABLE IF EXISTS `cs_conversation`;
CREATE TABLE `cs_conversation`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `app_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'AI应用ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户名称',
  `user_avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像',
  `user_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户IP',
  `user_device` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户设备信息(User-Agent)',
  `user_os` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统',
  `user_os_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作系统版本',
  `user_browser` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器',
  `user_browser_version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器版本',
  `user_device_id` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备指纹',
  `user_country` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国家',
  `user_province` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省份',
  `user_city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '城市',
  `user_lang` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '浏览器语言',
  `agent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '主负责客服ID',
  `status` tinyint NULL DEFAULT 0 COMMENT '状态: 0-未分配 1-已分配 2-已结束',
  `reply_mode` tinyint NULL DEFAULT 0 COMMENT '回复模式: 0-AI自动 1-手动 2-AI辅助',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源渠道',
  `last_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '最后一条消息',
  `last_message_time` datetime NULL DEFAULT NULL COMMENT '最后消息时间',
  `visitor_last_msg_time` datetime NULL DEFAULT NULL COMMENT '访客最后发消息时间(NULL表示客服已回复)',
  `unread_count` int NULL DEFAULT 0 COMMENT '客服未读消息数',
  `message_count` int NULL DEFAULT 0 COMMENT '总消息数',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `assign_time` datetime NULL DEFAULT NULL COMMENT '客服接入时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `satisfaction` tinyint NULL DEFAULT NULL COMMENT '满意度评分: 1-5',
  `satisfaction_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评价内容',
  `timeout_warned` tinyint(1) NULL DEFAULT 0 COMMENT '是否已发送超时提醒',
  `deleted` int NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_agent_status`(`agent_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_app_id`(`app_id` ASC) USING BTREE,
  INDEX `idx_deleted`(`deleted` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_conversation
-- ----------------------------

-- ----------------------------
-- Table structure for cs_domain_config
-- ----------------------------
DROP TABLE IF EXISTS `cs_domain_config`;
CREATE TABLE `cs_domain_config`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键',
  `domains` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '域名列表(换行分隔)',
  `download_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '桌面端下载链接',
  `download_links` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '下载链接列表(JSON)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态(1启用0禁用)',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除标记',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '域名配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_domain_config
-- ----------------------------

-- ----------------------------
-- Table structure for cs_global_config
-- ----------------------------
DROP TABLE IF EXISTS `cs_global_config`;
CREATE TABLE `cs_global_config`  (
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置Key',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置Value',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`config_key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服全局配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_global_config
-- ----------------------------
INSERT INTO `cs_global_config` VALUES ('agent_ip_whitelist_enabled', 'false', '2026-02-28 14:52:00', '2026-02-28 14:52:00');
INSERT INTO `cs_global_config` VALUES ('ai_enabled', 'true', '2026-02-28 14:52:00', '2026-02-28 14:52:00');
INSERT INTO `cs_global_config` VALUES ('auto_messages', '{\"languages\":{\"zh-CN\":{\"label\":\"中文简体\",\"messages\":[{\"content\":\"\"},{\"content\":\"\"}]},\"zh-TW\":{\"label\":\"中文繁體\",\"messages\":[]},\"en\":{\"label\":\"English\",\"messages\":[]}},\"defaultLang\":\"zh-CN\"}', '2026-02-28 14:52:00', '2026-02-28 14:55:19');
INSERT INTO `cs_global_config` VALUES ('chat_window_settings', '{\"themeColor\":\"#5866a7\",\"headerVisible\":true,\"pageTitle\":\"在线客服\",\"logo\":\"\",\"agentBubbleBgColor\":\"#f9cdcd\",\"agentBubbleFontColor\":\"#000000\",\"visitorBubbleBgColor\":\"#c7e9ff\",\"visitorBubbleFontColor\":\"#000000\",\"visitorAvatar\":\"\",\"visitorHistory\":true,\"visitorMessageConnect\":false,\"sendEmoji\":true,\"sendImage\":true,\"sendVideo\":true,\"sendPdf\":true,\"maxFileSize\":15,\"visitorTimezone\":\"auto\",\"scrollText\":\"\",\"scrollDuration\":10,\"scrollTextColor\":\"#ffffff\",\"scrollTextBgColor\":\"#1890ff\",\"backgroundImage\":\"\",\"pcAdLink\":\"\",\"pcAdImage\":\"\",\"headerBgImage\":\"\",\"headerIcons\":[],\"faqEnabled\":false,\"faqList\":[]}', '2026-02-28 14:56:21', '2026-02-28 14:59:11');
INSERT INTO `cs_global_config` VALUES ('conversation_assign', '{\"assignMode\":\"saturation\",\"inheritLastAgent\":{\"enabled\":true,\"expireMinutes\":60},\"conversationHold\":{\"minutes\":10},\"agentTimeoutReminder\":{\"enabled\":false,\"seconds\":20}}', '2026-02-28 14:52:00', '2026-02-28 14:52:00');
INSERT INTO `cs_global_config` VALUES ('message_board', '{\"subtitle\":\"客服不在线，请留言\",\"fields\":{\"name\":{\"show\":true,\"required\":true},\"phone\":{\"show\":true,\"required\":false},\"email\":{\"show\":true,\"required\":false},\"qq\":{\"show\":false,\"required\":false},\"wechat\":{\"show\":false,\"required\":false},\"image\":{\"show\":true,\"required\":false}}}', '2026-02-28 14:52:00', '2026-02-28 14:52:00');
INSERT INTO `cs_global_config` VALUES ('sensitive_words', '{\"enabled\":false,\"words\":[\"敏感词1\"]}', '2026-02-28 14:59:30', '2026-02-28 14:59:30');

-- ----------------------------
-- Table structure for cs_ip_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `cs_ip_blacklist`;
CREATE TABLE `cs_ip_blacklist`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'IP或CIDR段',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拉黑原因',
  `operator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人',
  `ban_date` datetime NULL DEFAULT NULL COMMENT '拉黑日期',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ip`(`ip` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '访客IP黑名单' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_ip_blacklist
-- ----------------------------

-- ----------------------------
-- Table structure for cs_ip_geo_cache
-- ----------------------------
DROP TABLE IF EXISTS `cs_ip_geo_cache`;
CREATE TABLE `cs_ip_geo_cache`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'IP地址',
  `country` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '国家',
  `province` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '省份',
  `city` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '城市',
  `raw_response` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'API原始响应JSON',
  `create_time` datetime NULL DEFAULT NULL COMMENT '缓存时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_ip`(`ip` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'IP地理位置缓存表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_ip_geo_cache
-- ----------------------------

-- ----------------------------
-- Table structure for cs_leave_message
-- ----------------------------
DROP TABLE IF EXISTS `cs_leave_message`;
CREATE TABLE `cs_leave_message`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访客用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '留言内容',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `phone` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `qq` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'QQ',
  `wechat` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片URL',
  `reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '客服回复',
  `reply_agent_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '回复客服ID',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `status` int NULL DEFAULT 0 COMMENT '0-待回复 1-已回复',
  `user_read` tinyint NULL DEFAULT 0 COMMENT '用户是否已读回复',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客服留言表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_leave_message
-- ----------------------------

-- ----------------------------
-- Table structure for cs_quick_reply
-- ----------------------------
DROP TABLE IF EXISTS `cs_quick_reply`;
CREATE TABLE `cs_quick_reply`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `category_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '标题/关键词',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '回复内容',
  `msg_type` tinyint NULL DEFAULT 0 COMMENT '消息类型: 0-文本 1-图片 2-文件 5-富文本',
  `extra` json NULL COMMENT '扩展信息',
  `use_count` int NULL DEFAULT 0 COMMENT '使用次数',
  `agent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属客服(NULL表示公共)',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_agent`(`agent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '快捷回复表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_quick_reply
-- ----------------------------
INSERT INTO `cs_quick_reply` VALUES ('1', '2', '您好', '您好，很高兴为您服务，请问有什么可以帮您？', 0, NULL, 0, NULL, 1, 0, NULL, '2026-01-12 14:33:41', NULL, NULL);
INSERT INTO `cs_quick_reply` VALUES ('2', '2', '稍等', '好的，请您稍等，我帮您查询一下。', 0, NULL, 0, NULL, 1, 1, NULL, '2026-01-12 14:33:41', NULL, NULL);
INSERT INTO `cs_quick_reply` VALUES ('3', '3', '感谢', '感谢您的咨询，祝您生活愉快！', 0, NULL, 0, NULL, 1, 0, NULL, '2026-01-12 14:33:41', NULL, NULL);
INSERT INTO `cs_quick_reply` VALUES ('4', '3', '再见', '感谢您的支持，如有问题随时联系我们，再见！', 0, NULL, 0, NULL, 1, 1, NULL, '2026-01-12 14:33:41', NULL, NULL);

-- ----------------------------
-- Table structure for cs_quick_reply_category
-- ----------------------------
DROP TABLE IF EXISTS `cs_quick_reply_category`;
CREATE TABLE `cs_quick_reply_category`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `parent_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父级ID',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '快捷回复分类表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_quick_reply_category
-- ----------------------------
INSERT INTO `cs_quick_reply_category` VALUES ('1', '常用语', NULL, 0, NULL, '2026-01-12 14:33:41');
INSERT INTO `cs_quick_reply_category` VALUES ('2', '问候语', '1', 0, NULL, '2026-01-12 14:33:41');
INSERT INTO `cs_quick_reply_category` VALUES ('3', '结束语', '1', 1, NULL, '2026-01-12 14:33:41');
INSERT INTO `cs_quick_reply_category` VALUES ('4', '产品咨询', NULL, 1, NULL, '2026-01-12 14:33:41');
INSERT INTO `cs_quick_reply_category` VALUES ('5', '售后服务', NULL, 2, NULL, '2026-01-12 14:33:41');

-- ----------------------------
-- Table structure for cs_visitor
-- ----------------------------
DROP TABLE IF EXISTS `cs_visitor`;
CREATE TABLE `cs_visitor`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键ID',
  `app_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '所属应用ID',
  `user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '访客唯一标识(关联会话表的user_id)',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注昵称',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别: 0-未知 1-男 2-女',
  `company` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '公司/组织',
  `position` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '职位',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `tags` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '标签(JSON数组)',
  `source` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '首次来源渠道',
  `first_visit_time` datetime NULL DEFAULT NULL COMMENT '首次访问时间',
  `last_visit_time` datetime NULL DEFAULT NULL COMMENT '最后访问时间',
  `visit_count` int NULL DEFAULT 1 COMMENT '访问次数',
  `conversation_count` int NULL DEFAULT 0 COMMENT '总会话数',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '详细备注',
  `level` tinyint NULL DEFAULT 1 COMMENT '客户等级: 1-普通 2-重要 3-VIP',
  `star` tinyint NULL DEFAULT 0 COMMENT '星标: 0-否 1-是',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_app_user`(`app_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE,
  INDEX `idx_star`(`star` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '访客信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_visitor
-- ----------------------------

-- ----------------------------
-- Table structure for cs_visitor_blacklist
-- ----------------------------
DROP TABLE IF EXISTS `cs_visitor_blacklist`;
CREATE TABLE `cs_visitor_blacklist`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '主键',
  `visitor_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '访客外部用户ID',
  `visitor_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '访客名称',
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '拉黑原因',
  `operator` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人',
  `ban_date` datetime NULL DEFAULT NULL COMMENT '拉黑日期',
  `last_visit_time` datetime NULL DEFAULT NULL COMMENT '最近访问时间',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_visitor_id`(`visitor_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '访客黑名单' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cs_visitor_blacklist
-- ----------------------------

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS `flyway_schema_history`;
CREATE TABLE `flyway_schema_history`  (
  `installed_rank` int NOT NULL,
  `version` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `script` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `checksum` int NULL DEFAULT NULL,
  `installed_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`) USING BTREE,
  INDEX `flyway_schema_history_s_idx`(`success` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of flyway_schema_history
-- ----------------------------
INSERT INTO `flyway_schema_history` VALUES (1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, 'root', '2024-01-03 21:58:35', 0, 1);
INSERT INTO `flyway_schema_history` VALUES (2, '3.8.0.1', 'airag add menu', 'SQL', 'V3.8.0_1__airag_add_menu.sql', -177373739, 'root', '2025-04-03 10:54:32', 114, 1);
INSERT INTO `flyway_schema_history` VALUES (3, '3.8.0.2', 'airag init db', 'SQL', 'V3.8.0_2__airag_init_db.sql', 874980827, 'root', '2025-04-07 14:35:13', 60, 1);
INSERT INTO `flyway_schema_history` VALUES (4, '3.8.1.1', 'all upgrade', 'SQL', 'V3.8.1_1__all_upgrade.sql', 670374510, 'root', '2025-06-25 15:09:03', 25, 1);
INSERT INTO `flyway_schema_history` VALUES (5, '3.8.1.2', 'openapi', 'SQL', 'V3.8.1_2__openapi.sql', 453642872, 'root', '2025-07-02 10:11:50', 245, 1);
INSERT INTO `flyway_schema_history` VALUES (6, '3.8.2.1', 'all upgrade', 'SQL', 'V3.8.2_1__all_upgrade.sql', 1279027750, 'root', '2025-07-30 18:13:06', 23, 1);
INSERT INTO `flyway_schema_history` VALUES (7, '3.8.3.0', 'all upgrade', 'SQL', 'V3.8.3_0__all_upgrade.sql', 1420195670, 'root', '2025-09-13 17:06:41', 22, 1);
INSERT INTO `flyway_schema_history` VALUES (8, '3.8.3.1', 'upgrade jimubi', 'SQL', 'V3.8.3_1__upgrade_jimubi.sql', -1274458791, 'root', '2025-11-25 15:42:34', 13, 1);
INSERT INTO `flyway_schema_history` VALUES (9, '3.9.0.0', 'all upgrade', 'SQL', 'V3.9.0_0__all_upgrade.sql', -758666487, 'root', '2025-11-26 13:40:20', 48, 1);
INSERT INTO `flyway_schema_history` VALUES (10, '3.9.0.1', 'mcp demo', 'SQL', 'V3.9.0_1__mcp_demo.sql', -790563395, 'root', '2025-11-27 18:16:00', 18, 1);
INSERT INTO `flyway_schema_history` VALUES (11, '3.9.0.2', 'upd dep category', 'SQL', 'V3.9.0_2__upd_dep_category.sql', -71250240, 'root', '2025-11-27 18:45:48', 19, 1);
INSERT INTO `flyway_schema_history` VALUES (12, '3.9.0.3', 'add aiflow permission', 'SQL', 'V3.9.0_3__add_aiflow_permission.sql', 1502182637, 'root', '2025-12-01 15:13:59', 9, 1);

-- ----------------------------
-- Table structure for oauth2_registered_client
-- ----------------------------
DROP TABLE IF EXISTS `oauth2_registered_client`;
CREATE TABLE `oauth2_registered_client`  (
  `id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `client_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `client_id_issued_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `client_secret` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `client_secret_expires_at` timestamp NULL DEFAULT NULL,
  `client_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `client_authentication_methods` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `authorization_grant_types` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `redirect_uris` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `post_logout_redirect_uris` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scopes` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `client_settings` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `token_settings` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oauth2_registered_client
-- ----------------------------
INSERT INTO `oauth2_registered_client` VALUES ('3eacac0e-0de9-4727-9a64-6bdd4be2ee1f', 'jeecg-client', '2024-02-29 18:16:33', 'secret', NULL, '3eacac0e-0de9-4727-9a64-6bdd4be2ee1f', 'client_secret_basic', 'refresh_token,authorization_code,password,app,phone,social', 'http://127.0.0.1:8080/jeecg-', 'http://127.0.0.1:8080/', '*', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.client.require-proof-key\":false,\"settings.client.require-authorization-consent\":true}', '{\"@class\":\"java.util.Collections$UnmodifiableMap\",\"settings.token.reuse-refresh-tokens\":true,\"settings.token.id-token-signature-algorithm\":[\"org.springframework.security.oauth2.jose.jws.SignatureAlgorithm\",\"RS256\"],\"settings.token.access-token-time-to-live\":[\"java.time.Duration\",300000.000000000],\"settings.token.access-token-format\":{\"@class\":\"org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat\",\"value\":\"self-contained\"},\"settings.token.refresh-token-time-to-live\":[\"java.time.Duration\",3600.000000000],\"settings.token.authorization-code-time-to-live\":[\"java.time.Duration\",300000.000000000],\"settings.token.device-code-time-to-live\":[\"java.time.Duration\",300000.000000000]}');

-- ----------------------------
-- Table structure for oss_file
-- ----------------------------
DROP TABLE IF EXISTS `oss_file`;
CREATE TABLE `oss_file`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `file_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `url` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件地址',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = 'Oss File' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of oss_file
-- ----------------------------
INSERT INTO `oss_file` VALUES ('1739471555639607297', 'LOGO-mini.png', 'http://minio.jeecg.com/jeecgtest/LOGO-mini_1703557309951.png', 'jeecg', '2023-12-26 10:21:51', NULL, NULL);
INSERT INTO `oss_file` VALUES ('1739471895508254721', '儿童身高.png', 'http://minio.jeecg.com/jeecgtest/儿童身高_1703557391840.png', 'jeecg', '2023-12-26 10:23:12', NULL, NULL);
INSERT INTO `oss_file` VALUES ('1739472521256468482', '大屏.png', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/upload/test/大屏_1703557541165.png', 'jeecg', '2023-12-26 10:25:42', NULL, NULL);
INSERT INTO `oss_file` VALUES ('1739472619130552321', '大屏11.png', 'https://jeecgdev.oss-cn-beijing.aliyuncs.com/upload/test/大屏11_1703557564068.png', 'jeecg', '2023-12-26 10:26:05', NULL, NULL);

-- ----------------------------
-- Table structure for qrtz_blob_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_blob_triggers`;
CREATE TABLE `qrtz_blob_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `BLOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `SCHED_NAME`(`SCHED_NAME` ASC, `TRIGGER_NAME` ASC, `TRIGGER_GROUP` ASC) USING BTREE,
  CONSTRAINT `QRTZ_BLOB_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_blob_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_calendars
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_calendars`;
CREATE TABLE `qrtz_calendars`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `CALENDAR_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_calendars
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_cron_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_cron_triggers`;
CREATE TABLE `qrtz_cron_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `CRON_EXPRESSION` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TIME_ZONE_ID` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_CRON_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_cron_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_fired_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_fired_triggers`;
CREATE TABLE `qrtz_fired_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `ENTRY_ID` varchar(95) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `INSTANCE_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `JOB_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`) USING BTREE,
  INDEX `IDX_QRTZ_FT_TRIG_INST_NAME`(`SCHED_NAME` ASC, `INSTANCE_NAME` ASC) USING BTREE,
  INDEX `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY`(`SCHED_NAME` ASC, `INSTANCE_NAME` ASC, `REQUESTS_RECOVERY` ASC) USING BTREE,
  INDEX `IDX_QRTZ_FT_J_G`(`SCHED_NAME` ASC, `JOB_NAME` ASC, `JOB_GROUP` ASC) USING BTREE,
  INDEX `IDX_QRTZ_FT_JG`(`SCHED_NAME` ASC, `JOB_GROUP` ASC) USING BTREE,
  INDEX `IDX_QRTZ_FT_T_G`(`SCHED_NAME` ASC, `TRIGGER_NAME` ASC, `TRIGGER_GROUP` ASC) USING BTREE,
  INDEX `IDX_QRTZ_FT_TG`(`SCHED_NAME` ASC, `TRIGGER_GROUP` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_fired_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_job_details
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_job_details`;
CREATE TABLE `qrtz_job_details`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `IS_DURABLE` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `IS_NONCONCURRENT` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `IS_UPDATE_DATA` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_job_details
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_locks
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_locks`;
CREATE TABLE `qrtz_locks`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `LOCK_NAME` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_locks
-- ----------------------------
INSERT INTO `qrtz_locks` VALUES ('MyScheduler', 'STATE_ACCESS');
INSERT INTO `qrtz_locks` VALUES ('MyScheduler', 'TRIGGER_ACCESS');

-- ----------------------------
-- Table structure for qrtz_paused_trigger_grps
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
CREATE TABLE `qrtz_paused_trigger_grps`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_GROUP`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_paused_trigger_grps
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_scheduler_state
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_scheduler_state`;
CREATE TABLE `qrtz_scheduler_state`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `INSTANCE_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_scheduler_state
-- ----------------------------
INSERT INTO `qrtz_scheduler_state` VALUES ('MyScheduler', 'DESKTOP-RSN9VAQ1772260271212', 1772262230229, 15000);

-- ----------------------------
-- Table structure for qrtz_simple_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simple_triggers`;
CREATE TABLE `qrtz_simple_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_simple_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_simprop_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
CREATE TABLE `qrtz_simprop_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `STR_PROP_1` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `STR_PROP_2` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `STR_PROP_3` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `INT_PROP_1` int NULL DEFAULT NULL,
  `INT_PROP_2` int NULL DEFAULT NULL,
  `LONG_PROP_1` bigint NULL DEFAULT NULL,
  `LONG_PROP_2` bigint NULL DEFAULT NULL,
  `DEC_PROP_1` decimal(13, 4) NULL DEFAULT NULL,
  `DEC_PROP_2` decimal(13, 4) NULL DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_simprop_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for qrtz_triggers
-- ----------------------------
DROP TABLE IF EXISTS `qrtz_triggers`;
CREATE TABLE `qrtz_triggers`  (
  `SCHED_NAME` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `JOB_GROUP` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `DESCRIPTION` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint NULL DEFAULT NULL,
  `PREV_FIRE_TIME` bigint NULL DEFAULT NULL,
  `PRIORITY` int NULL DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `TRIGGER_TYPE` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint NULL DEFAULT NULL,
  `CALENDAR_NAME` varchar(190) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `MISFIRE_INSTR` smallint NULL DEFAULT NULL,
  `JOB_DATA` blob NULL,
  PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) USING BTREE,
  INDEX `IDX_QRTZ_T_J`(`SCHED_NAME` ASC, `JOB_NAME` ASC, `JOB_GROUP` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_JG`(`SCHED_NAME` ASC, `JOB_GROUP` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_C`(`SCHED_NAME` ASC, `CALENDAR_NAME` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_G`(`SCHED_NAME` ASC, `TRIGGER_GROUP` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_STATE`(`SCHED_NAME` ASC, `TRIGGER_STATE` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_N_STATE`(`SCHED_NAME` ASC, `TRIGGER_NAME` ASC, `TRIGGER_GROUP` ASC, `TRIGGER_STATE` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_N_G_STATE`(`SCHED_NAME` ASC, `TRIGGER_GROUP` ASC, `TRIGGER_STATE` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_NEXT_FIRE_TIME`(`SCHED_NAME` ASC, `NEXT_FIRE_TIME` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_ST`(`SCHED_NAME` ASC, `TRIGGER_STATE` ASC, `NEXT_FIRE_TIME` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_MISFIRE`(`SCHED_NAME` ASC, `MISFIRE_INSTR` ASC, `NEXT_FIRE_TIME` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_ST_MISFIRE`(`SCHED_NAME` ASC, `MISFIRE_INSTR` ASC, `NEXT_FIRE_TIME` ASC, `TRIGGER_STATE` ASC) USING BTREE,
  INDEX `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP`(`SCHED_NAME` ASC, `MISFIRE_INSTR` ASC, `NEXT_FIRE_TIME` ASC, `TRIGGER_GROUP` ASC, `TRIGGER_STATE` ASC) USING BTREE,
  CONSTRAINT `QRTZ_TRIGGERS_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of qrtz_triggers
-- ----------------------------

-- ----------------------------
-- Table structure for sys_announcement
-- ----------------------------
DROP TABLE IF EXISTS `sys_announcement`;
CREATE TABLE `sys_announcement`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `titile` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标题',
  `msg_content` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '内容',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `sender` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '发布人',
  `priority` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '优先级（L低，M中，H高）',
  `msg_category` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '2' COMMENT '消息类型1:通知公告2:系统消息',
  `msg_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '通告对象类型（USER:指定用户，ALL:全体用户）',
  `send_status` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '发布状态（0未发布，1已发布，2已撤销）',
  `send_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `cancel_time` datetime NULL DEFAULT NULL COMMENT '撤销时间',
  `del_flag` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '删除状态（0，正常，1已删除）',
  `bus_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '业务类型(email:邮件 bpm:流程 tenant_invite:租户邀请)',
  `bus_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '业务id',
  `open_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '打开方式(组件：component 路由：url)',
  `open_page` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '组件/路由 地址',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `user_ids` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '指定用户',
  `msg_abstract` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '摘要/扩展业务参数',
  `dt_task_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '钉钉task_id，用于撤回消息',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  `files` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '附件',
  `visits_num` int NULL DEFAULT NULL COMMENT '访问次数',
  `iz_top` int NULL DEFAULT 0 COMMENT '是否置顶（0:否;  1:是）',
  `iz_approval` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否审批（0否 1是）',
  `bpm_status` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '流程状态',
  `msg_classify` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '消息归类',
  `notice_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '通知类型(system:系统消息、file:知识库、flow:流程、plan:日程计划、meeting:会议)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sanno_endtime`(`end_time` ASC) USING BTREE,
  INDEX `idx_sanno_start_time`(`start_time` ASC) USING BTREE,
  INDEX `idx_sanno_msg_type`(`msg_type` ASC) USING BTREE,
  INDEX `idx_sanno_send_status`(`send_status` ASC) USING BTREE,
  INDEX `idx_sanno_del_flag`(`del_flag` ASC) USING BTREE,
  INDEX `idx_sanno_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_sanno_sender`(`sender` ASC) USING BTREE,
  INDEX `idx_sanno_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统通告表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_announcement
-- ----------------------------
INSERT INTO `sys_announcement` VALUES ('1950442031319511042', '要放假了', '<p>我们都知道, 只要有意义, 那么就必须慎重考虑.既然如何, 带着这些问题, 我们来审视一下随机一段废话. 从这个角度来看, 在这种不可避免的冲突下，我们必须解决这个问题. 本人也是经过了深思熟虑,在每个日日夜夜思考这个问题. 随机一段废话因何而发生?<br>我们都知道, 只要有意义, 那么就必须慎重考虑.从这个角度来看, 从这个角度来看,&nbsp;<br>现在, 解决随机一段废话的问题, 是非常非常重要的. 所以</p>', NULL, NULL, 'admin', 'H', '1', 'ALL', '1', '2025-07-30 14:23:14', NULL, '0', NULL, NULL, NULL, NULL, 'admin', '2025-07-30 14:23:12', 'admin', '2025-07-30 14:23:14', '', '我们都知道, 只要有意义, 那么就必须慎重考虑', NULL, 0, NULL, NULL, 0, '0', '1', NULL, 'system');
INSERT INTO `sys_announcement` VALUES ('1950447687359426562', '又又更新！JeecgBootv3.8.2Online专项升级来袭，引领AI低代码平台新时代～', '<h2 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 0px 0px 16px; font-weight: 600; padding: 0px; font-size: 24px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">项目介绍</h2>\n<blockquote style=\"box-sizing: inherit; position: relative; margin: 0px 0px 20px; padding: 20px; background-color: rgb(246, 246, 246); border-left: 6px solid rgb(230, 230, 230); font-style: normal; font-weight: 400; word-break: break-word; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<p style=\"box-sizing: inherit; margin: 0px; line-height: inherit;\">JeecgBoot 是一款集成 AI 应用的，基于 BPM 流程的低代码平台，旨在帮助开发者快速实现低代码开发和构建、部署个性化的 AI 应用。 前后端分离架构 Ant Design&amp;Vue3，SpringBoot，SpringCloud，Mybatis，Shiro，强大的代码生成器让前后端代码一键生成，无需写任何代码！ 成套 AI 大模型功能: AI 模型、AI 应用、知识库、AI 流程编排、AI 对话等； 引领 AI 低代码开发模式， 帮助 Java 项目解决 80% 的重复工作，让开发更多关注业务，提高效率，同时又不失灵活性！</p>\n</blockquote>\n<p style=\"box-sizing: inherit; margin: 0px 0px 20px; line-height: inherit; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\"><strong style=\"box-sizing: inherit; font-weight: bold;\">发版时间</strong>：v3.8.2 | 2025-08-04</p>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">源码下载</h4>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\">Github 地址：<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">https://github.com/jeecgboot/JeecgBoot</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\">Gitee 地址：<span>&nbsp;</span><a href=\"https://gitee.com/jeecg/JeecgBoot\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\">https://gitee.com/jeecg/JeecgBoot</a></li>\n</ul>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">升级日志</h4>\n<blockquote style=\"box-sizing: inherit; position: relative; margin: 0px 0px 20px; padding: 20px; background-color: rgb(246, 246, 246); border-left: 6px solid rgb(230, 230, 230); font-style: normal; font-weight: 400; word-break: break-word; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<p style=\"box-sizing: inherit; margin: 0px; line-height: inherit;\">重点升级 Online 在线开发功能，支持 AI 建表、AI 生成报表；代码生成支持自定义按钮和 js 增强的结构方法生成，便于用户填充逻辑；</p>\n</blockquote>\n<h5 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 18px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">修复 issue</h5>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\">【架构升级】升级 mybatis-plus 到 3.5.12、升级 jsqlparser 到 4.9</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【架构升级】升级 jimureport 到 v2.1.1</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【架构升级】升级 jimubi 到 v2.1.0</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【架构升级】升级 online 到 3.8.2-beta</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【新功能】APP 版本管理功能，支持 app 自动补丁升级</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【新功能】独立门户设置菜单，支持角色和用户两个维度自定义首页</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【新功能】online 报表支持 AI 生成报表</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【新功能】online 功能测试支持 Mock 数据</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【新功能】online 表单支持 AI 建表</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【新功能】online 表单视图支持删除</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【新功能】online 生成的菜单 sql 自动带上组件名称</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【online 增强】支持页面 loading 调用</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【issues/8548】代码生成的高级查询里日期 - 月控件不能正常展示</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【issues/8163】关联记录新增丢失</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【issues/8575】erp 默认选中第一个及没选中主表时子表不查询</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【issues/8168】id 重复排序数据重了</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【issues/888】online 树表子节点搜索不生效且有警告</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【严重 bug】xxljob 和 jeecg-boot-starter 的 xxl-job 版本不一致，定时任务执行后任务不会结束</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">redis 缓存扩展用户可以自定义缓存</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">Customize redis listener handle 提醒日志，改成只提示一次</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">【issues/8265】默认模型支持超时时间设置</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">jeecg-boot-vue3 前端构建报错解决方案・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8497\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8497</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">Form 表单，点击 label 会 focus 到表格查询条件的 input 框上・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8484\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8484</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">Online 表单开发・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8286\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8286</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">online 表单生成问题・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8441\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8441</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">能不能继承 app 端版本管理・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8362\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8362</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">关于 AI 应用回复模型无法访问导致潜在问题・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8440\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8440</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">低代码开发 Online 表单开发主子表 ERP 显示问题・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8532\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8532</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">ApiSelect 分页加载重复请求问题・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8527\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8527</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">启动 democloud 服务时出现循环依赖报错・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8573\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8573</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">useScript.ts 的 isLoading 默认值应该是 true・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8552\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8552</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">BasicTable 不支持 TableLayout 设置 (默认写死 fixed)・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8564\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8564</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">严重：修改密码存在水平越权问题。・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8567\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8567</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">大模型设置 timeout，没有生效，并且总在报错超时之后，才返回相应结果・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8557\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8557</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">批量删除优化反馈・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8558\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8558</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">yml 文件中的 quart 配置 initialize-schema 属性层级错误・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8540\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8540</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">xxljob 和 jeecg-boot-starter 的 xxl-job 版本不一致，定时任务执行后任务不会结束・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8621\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8621</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">online 表单 一对多 erp 模式下子表刷新问题・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8575\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8575</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">springboot3 分支，knife4j 不能正确显示文档，但是 swagger-ui 和 v3/api-docs 正常・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8638\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8638</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">Swagger3 接口文档异常・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8631\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8631</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">springboot3 版本 3.8.1， knife4j-production 不生效・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8624\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8624</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">列权限控制问题・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8518\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8518</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\">行尾合计汇总问题・<a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2F8502\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Issue #8502</a></li>\n</ul>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">技术交流</h4>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\">官方网站：<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=http%3A%2F%2Fwww.jeecg.com\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">http://www.jeecg.com</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">在线演示：<a href=\"https://www.oschina.net/action/GoToLink?url=http%3A%2F%2Fboot3.jeecg.com\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">http://boot3.jeecg.com</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">入门指南：<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=http%3A%2F%2Fwww.jeecg.com%2Fdoc%2Fquickstart\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">快速入门</a><span>&nbsp;</span>|<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fhelp.jeecg.com\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">开发文档</a><span>&nbsp;</span>|<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fhelp.jeecg.com%2Faigc\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">AI 应用使用手册</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\">技术支持：<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fgithub.com%2Fjeecgboot%2FJeecgBoot%2Fissues%2Fnew%3Ftemplate%3Dbug_report.md\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">反馈问题</a><span>&nbsp;</span>|<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=http%3A%2F%2Fjeecg.com%2Fdoc%2Fvideo\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">视频教程</a><span>&nbsp;</span>|<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fjeecg.blog.csdn.net%2Farticle%2Fdetails%2F106079007\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">低代码体验一分钟</a></li>\n</ul>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">快速启动项目</h4>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\"><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fhelp.jeecg.com%2Fjava%2Fsetup%2Fidea%2Fstartup\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">IDEA 启动前后端项目</a></li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\"><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fhelp.jeecg.com%2Fjava%2Fdocker%2Fquick\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">Docker 一键启动前后端</a></li>\n</ul>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">AI 应用平台介绍</h4>\n<p style=\"box-sizing: inherit; margin: 0px 0px 20px; line-height: inherit; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">JeecgBoot 平台提供了一套完善的 AI 应用管理系统模块，是一套类似<span>&nbsp;</span><code style=\"box-sizing: inherit; font-family: Consolas, Monaco, \'Andale Mono\', \'Ubuntu Mono\', monospace; font-size: 13px; margin: 0px 3px; padding: 3px 4px; border: none; border-radius: 4px; background: rgb(246, 246, 246); color: rgb(51, 51, 51);\">Dify</code><span>&nbsp;</span>的<span>&nbsp;</span><code style=\"box-sizing: inherit; font-family: Consolas, Monaco, \'Andale Mono\', \'Ubuntu Mono\', monospace; font-size: 13px; margin: 0px 3px; padding: 3px 4px; border: none; border-radius: 4px; background: rgb(246, 246, 246); color: rgb(51, 51, 51);\">AIGC应用开发平台</code><span>&nbsp;</span>+<span>&nbsp;</span><code style=\"box-sizing: inherit; font-family: Consolas, Monaco, \'Andale Mono\', \'Ubuntu Mono\', monospace; font-size: 13px; margin: 0px 3px; padding: 3px 4px; border: none; border-radius: 4px; background: rgb(246, 246, 246); color: rgb(51, 51, 51);\">知识库问答</code>，是一款基于 LLM 大语言模型 AI 应用平台和 RAG 的知识库问答系统。 其直观的界面结合了 AI 流程编排、RAG 管道、知识库管理、模型管理、对接向量库、实时运行可观察等，让您可以快速从原型到生产，拥有 AI 服务能力。<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fhelp.jeecg.com%2Faigc\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">详细专题介绍，请点击查看</a></p>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">适用项目</h4>\n<p style=\"box-sizing: inherit; margin: 0px 0px 20px; line-height: inherit; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">JeecgBoot 低代码平台，可以应用在任何 J2EE 项目的开发中，支持信创国产化。尤其适合 SAAS 项目、企业信息管理系统（MIS）、内部办公系统（OA）、企业资源计划系统（ERP）、客户关系管理系统（CRM）、AI 知识库等，其半智能手工 Merge 的开发方式，可以显著提高开发效率 70% 以上，极大降低开发成本。 又是一个全栈式 AI 开发平台，快速帮助企业构建和部署个性化的 AI 应用。</p>\n<p style=\"box-sizing: inherit; margin: 0px 0px 20px; line-height: inherit; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\"><strong style=\"box-sizing: inherit; font-weight: bold;\">信创兼容说明</strong></p>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\">操作系统：国产麒麟、银河麒麟等国产系统几乎都是基于 Linux 内核，因此它们具有良好的兼容性。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">数据库：达梦、人大金仓、TiDB</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\">中间件：东方通 TongWeb、TongRDS，宝兰德 AppServer、CacheDB,<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fhelp.jeecg.com%2Fjava%2Ftongweb-deploy%2F\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">信创配置文档</a></li>\n</ul>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">为什么选择 JeecgBoot?</h4>\n<blockquote style=\"box-sizing: inherit; position: relative; margin: 0px 0px 20px; padding: 20px; background-color: rgb(246, 246, 246); border-left: 6px solid rgb(230, 230, 230); font-style: normal; font-weight: 400; word-break: break-word; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<p style=\"box-sizing: inherit; margin: 0px; line-height: inherit;\">开源界 \"小普元\" 超越传统商业平台。引领低代码开发模式 (OnlineCoding-&gt; 代码生成器 -&gt; 手工 MERGE)，低代码开发同时又支持灵活编码， 可以帮助解决 Java 项目 70% 的重复工作，让开发更多关注业务。既能快速提高开发效率，节省成本，同时又不失灵活性。</p>\n</blockquote>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\">1. 采用最新主流前后分离框架（Spring Boot + MyBatis + Ant Design4 + Vue3），容易上手；代码生成器依赖性低，灵活的扩展能力，可快速实现二次开发。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">2. 前端大版本换代，最新版采用 Vue3.0 + TypeScript + Vite6 + Ant Design Vue4 等新技术方案。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">3. 支持微服务 Spring Cloud Alibaba（Nacos、Gateway、Sentinel、Skywalking），提供简易机制，支持单体和微服务自由切换（这样可以满足各类项目需求）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">4. 开发效率高，支持在线建表和 AI 建表，提供强大代码生成器，单表、树列表、一对多、一对一等数据模型，增删改查功能一键生成，菜单配置直接使用。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">5. 代码生成器提供强大模板机制，支持自定义模板，目前提供四套风格模板（单表两套、树模型一套、一对多三套）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">6. 提供强大的报表和大屏可视化工具，支持丰富的数据源连接，能够通过拖拉拽方式快速制作报表、大屏和门户设计；支持多种图表类型：柱形图、折线图、散点图、饼图、环形图、面积图、漏斗图、进度图、仪表盘、雷达图、地图等。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">7. 低代码能力：在线表单（无需编码，通过在线配置表单，实现表单的增删改查，支持单表、树、一对多、一对一等模型，实现人人皆可编码），在线配置零代码开发、所见即所得支持 23 种类控件。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">8. 低代码能力：在线报表、在线图表（无需编码，通过在线配置方式，实现数据报表和图形报表，可以快速抽取数据，减轻开发压力，实现人人皆可编码）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">9.Online 支持在线增强开发，提供在线代码编辑器，支持代码高亮、代码提示等功能，支持多种语言（Java、SQL、JavaScript 等）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">10. 封装完善的用户、角色、菜单、组织机构、数据字典、在线定时任务等基础功能，支持访问授权、按钮权限、数据权限等功能。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">11. 前端 UI 提供丰富的组件库，支持各种常用组件，如表格、树形控件、下拉框、日期选择器等，满足各种复杂的业务需求<span>&nbsp;</span><a href=\"https://www.oschina.net/action/GoToLink?url=https%3A%2F%2Fhelp.jeecg.com%2Fcategory%2Fui%25E7%25BB%2584%25E4%25BB%25B6%25E5%25BA%2593\" target=\"_blank\" style=\"box-sizing: inherit; background-color: transparent; color: rgb(52, 111, 182); text-decoration: none;\" rel=\"noopener\">UI 组件库文档</a>。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">12. 提供 APP 配套框架，一份多代码多终端适配，一份代码多终端适配，小程序、H5、安卓、iOS、鸿蒙 Next。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">13. 新版 APP 框架采用 Uniapp、Vue3.0、Vite、Wot-design-uni、TypeScript 等最新技术栈，包括二次封装组件、路由拦截、请求拦截等功能。实现了与 JeecgBoot 完美对接：目前已经实现登录、用户信息、通讯录、公告、移动首页、九宫格、聊天、Online 表单、仪表盘等功能，提供了丰富的组件。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">14. 提供了一套成熟的 AI 应用平台功能，从 AI 模型、知识库到 AI 应用搭建，助力企业快速落地 AI 服务，加速智能化升级。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">15.AI 能力：目前 JeecgBoot 支持 AI 大模型 chatgpt 和 deepseek，现在最新版默认使用 deepseek，速度更快质量更高。目前提供了 AI 对话助手、AI 知识库、AI 应用、AI 建表、AI 报表等功能。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">16. 提供新行编辑表格 JVXETable，轻松满足各种复杂 ERP 布局，拥有更高的性能、更灵活的扩展、更强大的功能。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">17. 平台首页风格，提供多种组合模式，支持自定义风格；支持门户设计，支持自定义首页。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">18. 常用共通封装，各种工具类（定时任务、短信接口、邮件发送、Excel 导入导出等），基本满足 80% 项目需求。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">19. 简易 Excel 导入导出，支持单表导出和一对多表模式导出，生成的代码自带导入导出功能。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">20. 集成智能报表工具，报表打印、图像报表和数据导出非常方便，可极其方便地生成 PDF、Excel、Word 等报表。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">21. 采用前后分离技术，页面 UI 风格精美，针对常用组件做了封装：时间、行表格控件、截取显示控件、报表组件、编辑器等。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">22. 查询过滤器：查询功能自动生成，后台动态拼 SQL 追加查询条件；支持多种匹配方式（全匹配 / 模糊查询 / 包含查询 / 不匹配查询）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">23. 数据权限（精细化数据权限控制，控制到行级、列表级、表单字段级，实现不同人看不同数据，不同人对同一个页面操作不同字段）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">24. 接口安全机制，可细化控制接口授权，非常简便实现不同客户端只看自己数据等控制；也提供了基于 AK 和 SK 认证鉴权的 OpenAPI 功能。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">25. 活跃的社区支持；近年来，随着网络威胁的日益增加，团队在安全和漏洞管理方面积累了丰富的经验，能够为企业提供全面的安全解决方案。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">26. 权限控制采用 RBAC（Role-Based Access Control，基于角色的访问控制）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">27. 页面校验自动生成（必须输入、数字校验、金额校验、时间空间等）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">28. 支持 SaaS 服务模式，提供 SaaS 多租户架构方案。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">29. 分布式文件服务，集成 MinIO、阿里 OSS 等优秀的第三方，提供便捷的文件上传与管理，同时也支持本地存储。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">30. 主流数据库兼容，一套代码完全兼容 MySQL、PostgreSQL、Oracle、SQL Server、MariaDB、达梦、人大金仓等主流数据库。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">31. 集成工作流 Flowable，并实现了只需在页面配置流程转向，可极大简化 BPM 工作流的开发；用 BPM 的流程设计器画出了流程走向，一个工作流基本就完成了，只需写很少量的 Java 代码。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">32. 低代码能力：在线流程设计，采用开源 Flowable 流程引擎，实现在线画流程、自定义表单、表单挂靠、业务流转。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">33. 多数据源：极其简易的使用方式，在线配置数据源配置，便捷地从其他数据抓取数据。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">34. 提供单点登录 CAS 集成方案，项目中已经提供完善的对接代码。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">35. 低代码能力：表单设计器，支持用户自定义表单布局，支持单表、一对多表单，支持 select、radio、checkbox、textarea、date、popup、列表、宏等控件。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">36. 专业接口对接机制，统一采用 RESTful 接口方式，集成 Swagger-UI 在线接口文档，JWT token 安全验证，方便客户端对接。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">37. 高级组合查询功能，在线配置支持主子表关联查询，可保存查询历史。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">38. 提供各种系统监控，实时跟踪系统运行情况（监控 Redis、Tomcat、JVM、服务器信息、请求追踪、SQL 监控）。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">39. 消息中心（支持短信、邮件、微信推送等）；集成 WebSocket 消息通知机制。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">40. 支持多语言，提供国际化方案。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">41. 数据变更记录日志，可记录数据每次变更内容，通过版本对比功能查看历史变化。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">42. 提供简单易用的打印插件，支持谷歌、火狐、IE11 + 等各种浏览器。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">43. 后端采用 Maven 分模块开发方式；前端支持菜单动态路由。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\">44. 提供丰富的示例代码，涵盖了常用的业务场景，便于学习和参考。</li>\n</ul>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">技术架构：</h4>\n<h6 style=\"box-sizing: inherit; font-weight: 600; line-height: 1.8; margin: 22px 0px 16px; padding: 0px; border: none; font-size: 16px; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">前端</h6>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\">前端环境要求：Node.js 要求<span>&nbsp;</span><code style=\"box-sizing: inherit; font-family: Consolas, Monaco, \'Andale Mono\', \'Ubuntu Mono\', monospace; font-size: 13px; margin: 0px 3px; padding: 3px 4px; border: none; border-radius: 4px; background: rgb(246, 246, 246); color: rgb(51, 51, 51);\">Node 20+</code><span>&nbsp;</span>版本以上、pnpm 要求<span>&nbsp;</span><code style=\"box-sizing: inherit; font-family: Consolas, Monaco, \'Andale Mono\', \'Ubuntu Mono\', monospace; font-size: 13px; margin: 0px 3px; padding: 3px 4px; border: none; border-radius: 4px; background: rgb(246, 246, 246); color: rgb(51, 51, 51);\">9+</code><span>&nbsp;</span>版本以上</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">依赖管理：node、npm、pnpm</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">前端 IDE 建议：IDEA、WebStorm、Vscode</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">采用 Vue3.0+TypeScript+Vite6+Ant-Design-Vue4 等新技术方案，包括二次封装组件、utils、hooks、动态菜单、权限校验、按钮级别权限控制等功能</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\">最新技术栈：Vue3.0 + TypeScript + Vite6 + ant-design-vue4 + pinia + echarts + unocss + vxe-table + qiankun + es6</li>\n</ul>\n<h6 style=\"box-sizing: inherit; font-weight: 600; line-height: 1.8; margin: 22px 0px 16px; padding: 0px; border: none; font-size: 16px; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">后端</h6>\n<ul style=\"box-sizing: inherit; padding-inline-start: 40px !important; margin: 0px 0px 20px; padding: 0px 0px 0px 20px; list-style-type: disc; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-top: 0px;\">IDE 建议： IDEA (必须安装 lombok 插件)</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">语言：Java 默认 jdk17 (支持 jdk8、jdk21)</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">依赖管理：Maven</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">基础框架：Spring Boot 2.7.18</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">微服务框架： Spring Cloud Alibaba 2021.0.6.2</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">持久层框架：MybatisPlus 3.5.3.2</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">报表工具： JimuReport 1.9.5</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">安全框架：Apache Shiro 1.13.0，Jwt 4.5.0</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">微服务技术栈：Spring Cloud Alibaba、Nacos、Gateway、Sentinel、Skywalking</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">数据库连接池：阿里巴巴 Druid 1.1.24</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">AI 大模型：支持<span>&nbsp;</span><code style=\"box-sizing: inherit; font-family: Consolas, Monaco, \'Andale Mono\', \'Ubuntu Mono\', monospace; font-size: 13px; margin: 0px 3px; padding: 3px 4px; border: none; border-radius: 4px; background: rgb(246, 246, 246); color: rgb(51, 51, 51);\">ChatGPT</code><span>&nbsp;</span><code style=\"box-sizing: inherit; font-family: Consolas, Monaco, \'Andale Mono\', \'Ubuntu Mono\', monospace; font-size: 13px; margin: 0px 3px; padding: 3px 4px; border: none; border-radius: 4px; background: rgb(246, 246, 246); color: rgb(51, 51, 51);\">DeepSeek</code><span>&nbsp;</span>切换</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">日志打印：logback</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">缓存：Redis</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em;\">其他：autopoi, fastjson，poi，Swagger-ui，quartz, lombok（简化代码）等。</li>\n<li style=\"box-sizing: inherit; line-height: 1.875em; margin-bottom: 0px;\">默认提供 MySQL5.7 + 数据库脚本</li>\n</ul>\n<h4 style=\"box-sizing: inherit; font-family: \'PingFang SC\', \'Helvetica Neue\', \'Microsoft YaHei UI\', \'Microsoft YaHei\', \'Noto Sans CJK SC\', Sathu, EucrosiaUPC, Arial, Helvetica, sans-serif; line-height: 1.8; margin: 22px 0px 16px; font-weight: 600; padding: 0px; font-size: 20px; border: none; color: rgb(51, 51, 51); font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\">微服务架构图</h4>\n<p style=\"box-sizing: inherit; margin: 0px 0px 20px; line-height: inherit; color: rgb(51, 51, 51); font-family: -apple-system, BlinkMacSystemFont, \'Apple Color Emoji\', \'Segoe UI Emoji\', \'Segoe UI Symbol\', \'Segoe UI\', \'PingFang SC\', \'Hiragino Sans GB\', \'Microsoft YaHei\', \'Helvetica Neue\', Helvetica, Arial, sans-serif; font-size: 16px; font-style: normal; font-variant-ligatures: normal; font-variant-caps: normal; font-weight: 400; letter-spacing: normal; orphans: 2; text-align: left; text-indent: 0px; text-transform: none; widows: 2; word-spacing: 0px; -webkit-text-stroke-width: 0px; white-space: normal; background-color: rgb(255, 255, 255); text-decoration-thickness: initial; text-decoration-style: initial; text-decoration-color: initial;\"><img src=\"https://oscimg.oschina.net/oscnet/up-32fae06d9114ee4fcbf3d2931be2ff02b28.png\" alt=\"\" class=\"zoom-in-cursor\" style=\"box-sizing: border-box; border: 0px; margin: 0px auto; max-width: 80%; height: auto; vertical-align: middle; cursor: zoom-in;\"></p>', NULL, NULL, 'admin', 'H', '1', 'ALL', '1', '2025-07-30 14:45:43', NULL, '0', NULL, NULL, NULL, NULL, 'admin', '2025-07-30 14:45:41', 'admin', '2025-07-30 14:45:43', '', '重点升级 Online 在线开发功能，支持 AI 建表、AI 生成报表；代码生成支持自定义按钮和 js 增强的结构方法生成，便于用户填充逻辑；', NULL, 0, NULL, NULL, 0, '0', '1', NULL, 'system');
INSERT INTO `sys_announcement` VALUES ('1957252479913807873', '要放假了', '<p>这样看来, 了解清楚随机一段废话到底是一种怎么样的存在, 是解决一切问题的关键.<br>而这些并不是完全重要, 更加重要的问题是, 所谓随机一段废话, 关键是随机一段废话需要如何写. 随机一段废话的发生, 到底需要如何做到, 不随机一段废话的发生, 又会如何产生. 生活中, 若随机一段废话出现了, 我们就不得不考虑它出现了的事实. 我们不妨可以这样来想:&nbsp;</p>\n<p>这样看来, 要想清楚, 随机一段废话,</p>', NULL, NULL, 'admin', 'H', '1', 'ALL', '1', '2025-08-18 09:25:32', NULL, '0', NULL, NULL, NULL, NULL, 'admin', '2025-08-18 09:25:30', 'admin', '2025-08-18 09:25:32', '', '国庆放假安排', NULL, 0, NULL, NULL, 0, '0', '1', NULL, 'system');
INSERT INTO `sys_announcement` VALUES ('1957253130366472194', '【重磅】JimuReport积木报表v2.0版本发布，免费可视化报表', '<p>【重磅】JimuReport 积木报表 v2.0 版本发布，免费可视化报表</p>', NULL, NULL, 'admin', 'H', '1', 'ALL', '1', '2025-08-18 09:28:20', NULL, '0', NULL, NULL, NULL, NULL, 'admin', '2025-08-18 09:28:05', 'admin', '2025-08-18 09:28:20', '', '【重磅】JimuReport 积木报表 v2.0 版本发布，免费可视化报表', NULL, 0, NULL, NULL, 0, '0', '1', NULL, 'system');
INSERT INTO `sys_announcement` VALUES ('1966817120570503169', '您已被 管理员 从 北京国炬信息技术有限公司中移除。', '您已被 管理员 从 北京国炬信息技术有限公司中移除。', NULL, NULL, 'system', 'M', '2', 'USER', '1', '2025-09-13 18:51:58', NULL, '0', NULL, NULL, NULL, NULL, 'admin', '2025-09-13 18:51:58', NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, 0, NULL, NULL, NULL, 'system');
INSERT INTO `sys_announcement` VALUES ('1993520100581388289', '还有两个月就过年了', '<p>本人也是经过了深思熟虑,在每个日日夜夜思考这个问题. 既然如何, 现在, 解决随机一段废话的问题, 是非常非常重要的. 所以, 我们不妨可以这样来想: 要想清楚, 随机一段废话, 到底是一种怎么样的存在. 要想清楚, 随机一段废话, 到底是一种怎么样的存在. 我们一般认为, 抓住了问题的关键, 其他一切则会迎刃而解.生活中, 若随机一段废话出现了, 我们就不得不考虑它出现了的事实. 我们都</p>', NULL, NULL, 'admin', 'H', '1', 'ALL', '1', '2025-11-26 11:20:08', NULL, '0', NULL, NULL, NULL, NULL, 'admin', '2025-11-26 11:20:04', 'admin', '2025-11-26 11:20:08', '', '还有两个月就过年了', NULL, 0, NULL, NULL, 0, '0', '1', NULL, 'system');
INSERT INTO `sys_announcement` VALUES ('1993520281771126785', '111', '<p>1111</p>', NULL, NULL, 'admin', 'H', '1', 'ALL', '1', '2025-11-26 11:20:52', NULL, '0', NULL, NULL, NULL, NULL, 'admin', '2025-11-26 11:20:48', 'admin', '2025-11-26 11:20:52', '', '111', NULL, 0, NULL, NULL, 0, '0', '1', NULL, 'system');

-- ----------------------------
-- Table structure for sys_announcement_send
-- ----------------------------
DROP TABLE IF EXISTS `sys_announcement_send`;
CREATE TABLE `sys_announcement_send`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `annt_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '通告ID',
  `user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `read_flag` int NULL DEFAULT NULL COMMENT '阅读状态（0未读，1已读）',
  `read_time` datetime NULL DEFAULT NULL COMMENT '阅读时间',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `star_flag` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '标星状态( 1为标星 空/0没有标星)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sacm_annt_id`(`annt_id` ASC) USING BTREE,
  INDEX `idx_sacm_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_sacm_read_flag`(`read_flag` ASC) USING BTREE,
  INDEX `idx_sacm_star_flag`(`star_flag` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户通告阅读标记表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_announcement_send
-- ----------------------------
INSERT INTO `sys_announcement_send` VALUES ('1950442039812976642', '1950442031319511042', '1714471285016895490', 0, NULL, 'admin', '2025-07-30 14:23:14', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1950442039812976643', '1950442031319511042', '3d464b4ea0d2491aab8a7bde74c57e95', 0, NULL, 'admin', '2025-07-30 14:23:14', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1950442039812976644', '1950442031319511042', 'a75d45a015c44384a04449ee80dc3503', 0, NULL, 'admin', '2025-07-30 14:23:14', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1950442039812976645', '1950442031319511042', 'e9ca23d68d884d4ebb19d07889727dae', 1, '2025-11-25 17:04:38', 'admin', '2025-07-30 14:23:14', 'admin', '2025-11-25 17:04:38', NULL);
INSERT INTO `sys_announcement_send` VALUES ('1950447696951799810', '1950447687359426562', '1714471285016895490', 0, NULL, 'admin', '2025-07-30 14:45:43', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1950447696968577026', '1950447687359426562', '3d464b4ea0d2491aab8a7bde74c57e95', 0, NULL, 'admin', '2025-07-30 14:45:43', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1950447696968577027', '1950447687359426562', 'a75d45a015c44384a04449ee80dc3503', 0, NULL, 'admin', '2025-07-30 14:45:43', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1950447696968577028', '1950447687359426562', 'e9ca23d68d884d4ebb19d07889727dae', 1, '2025-09-13 18:42:14', 'admin', '2025-07-30 14:45:43', 'admin', '2025-09-13 18:42:14', NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957252487350308865', '1957252479913807873', '1714471285016895490', 0, NULL, 'admin', '2025-08-18 09:25:32', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957252487358697473', '1957252479913807873', '1955218082645544962', 0, NULL, 'admin', '2025-08-18 09:25:32', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957252487358697474', '1957252479913807873', '1955218454478983170', 0, NULL, 'admin', '2025-08-18 09:25:32', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957252487358697475', '1957252479913807873', '3d464b4ea0d2491aab8a7bde74c57e95', 0, NULL, 'admin', '2025-08-18 09:25:32', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957252487358697476', '1957252479913807873', 'a75d45a015c44384a04449ee80dc3503', 0, NULL, 'admin', '2025-08-18 09:25:32', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957252487367086081', '1957252479913807873', 'e9ca23d68d884d4ebb19d07889727dae', 1, '2025-09-13 18:42:14', 'admin', '2025-08-18 09:25:32', 'admin', '2025-09-13 18:42:14', NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957253194027618305', '1957253130366472194', '1714471285016895490', 0, NULL, 'admin', '2025-08-18 09:28:20', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957253194031812610', '1957253130366472194', '1955218082645544962', 0, NULL, 'admin', '2025-08-18 09:28:20', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957253194031812611', '1957253130366472194', '1955218454478983170', 0, NULL, 'admin', '2025-08-18 09:28:20', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957253194031812612', '1957253130366472194', '3d464b4ea0d2491aab8a7bde74c57e95', 0, NULL, 'admin', '2025-08-18 09:28:20', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957253194031812613', '1957253130366472194', 'a75d45a015c44384a04449ee80dc3503', 0, NULL, 'admin', '2025-08-18 09:28:20', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1957253194036006913', '1957253130366472194', 'e9ca23d68d884d4ebb19d07889727dae', 1, '2025-11-25 17:04:35', 'admin', '2025-08-18 09:28:20', 'admin', '2025-11-25 17:04:35', NULL);
INSERT INTO `sys_announcement_send` VALUES ('1966817120570503170', '1966817120570503169', '1714471285016895490', 0, NULL, 'admin', '2025-09-13 18:51:58', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520115190149121', '1993520100581388289', '1714471285016895490', 0, NULL, 'admin', '2025-11-26 11:20:08', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520115198537729', '1993520100581388289', '3d464b4ea0d2491aab8a7bde74c57e95', 0, NULL, 'admin', '2025-11-26 11:20:08', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520115198537730', '1993520100581388289', 'a75d45a015c44384a04449ee80dc3503', 0, NULL, 'admin', '2025-11-26 11:20:08', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520115198537731', '1993520100581388289', 'e9ca23d68d884d4ebb19d07889727dae', 1, '2025-11-26 11:24:44', 'admin', '2025-11-26 11:20:08', 'admin', '2025-11-26 11:24:44', NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520300238647297', '1993520281771126785', '1714471285016895490', 0, NULL, 'admin', '2025-11-26 11:20:52', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520300247035906', '1993520281771126785', '3d464b4ea0d2491aab8a7bde74c57e95', 0, NULL, 'admin', '2025-11-26 11:20:52', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520300247035907', '1993520281771126785', 'a75d45a015c44384a04449ee80dc3503', 0, NULL, 'admin', '2025-11-26 11:20:52', NULL, NULL, NULL);
INSERT INTO `sys_announcement_send` VALUES ('1993520300255424513', '1993520281771126785', 'e9ca23d68d884d4ebb19d07889727dae', 1, '2025-11-26 11:24:42', 'admin', '2025-11-26 11:20:52', 'admin', '2025-11-26 11:24:42', NULL);

-- ----------------------------
-- Table structure for sys_category
-- ----------------------------
DROP TABLE IF EXISTS `sys_category`;
CREATE TABLE `sys_category`  (
  `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `pid` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父级节点',
  `name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型名称',
  `code` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型编码',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属部门',
  `has_child` varchar(3) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否有子节点',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `index_scg_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_category
-- ----------------------------
INSERT INTO `sys_category` VALUES ('1183693424827564034', '0', '物料树', 'B02', 'admin', '2019-10-14 18:37:59', 'admin', '2019-10-14 18:38:15', 'A01', '1', 0);
INSERT INTO `sys_category` VALUES ('1183693491043041282', '1183693424827564034', '上衣', 'B02A01', 'admin', '2019-10-14 18:38:15', 'admin', '2019-10-14 18:38:43', 'A01', '1', 0);
INSERT INTO `sys_category` VALUES ('1183693534173069314', '1183693424827564034', '裤子', 'B02A02', 'admin', '2019-10-14 18:38:25', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1183693610534567937', '1183693491043041282', '秋衣', 'B02A01A01', 'admin', '2019-10-14 18:38:43', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1183693700254924802', '1183693491043041282', '兵装', 'B02A01A02', 'admin', '2019-10-14 18:39:05', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1183693773974011906', '1183693491043041282', '女装', 'B02A01A03', 'admin', '2019-10-14 18:39:22', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1185039122143719425', '0', '电脑产品', 'A01', 'admin', '2019-10-18 11:45:18', 'admin', '2023-10-18 13:54:46', 'A01', '1', 0);
INSERT INTO `sys_category` VALUES ('1185039176799694850', '1185039122143719425', 'thinkpad', 'A01A01', 'admin', '2019-10-18 11:45:31', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1185039255115739138', '1185039122143719425', 'mackbook', 'A01A02', 'admin', '2019-10-18 11:45:50', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1185039299051073537', '1185039122143719425', '华为电脑', 'A01A03', 'admin', '2019-10-18 11:46:01', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1230769196661510146', '0', '省', NULL, 'admin', '2020-02-21 16:20:16', 'admin', '2020-02-21 16:20:31', 'A01A03', '1', 0);
INSERT INTO `sys_category` VALUES ('1230769253267836929', '1230769196661510146', '安徽省', NULL, 'admin', '2020-02-21 16:20:31', 'admin', '2020-02-21 16:20:53', 'A01A03', '1', 0);
INSERT INTO `sys_category` VALUES ('1230769290609725441', '1230769196661510146', '山东省', NULL, 'admin', '2020-02-21 16:20:40', 'admin', '2020-02-21 16:21:23', 'A01A03', '1', 0);
INSERT INTO `sys_category` VALUES ('1230769347157331969', '1230769253267836929', '合肥市', NULL, 'admin', '2020-02-21 16:20:53', 'admin', '2020-02-21 16:21:08', 'A01A03', '1', 0);
INSERT INTO `sys_category` VALUES ('1230769407907631106', '1230769347157331969', '包河区', NULL, 'admin', '2020-02-21 16:21:08', NULL, NULL, 'A01A03', NULL, 0);
INSERT INTO `sys_category` VALUES ('1230769470889299970', '1230769290609725441', '济南市', NULL, 'admin', '2020-02-21 16:21:23', 'admin', '2020-02-21 16:21:41', 'A01A03', '1', 0);
INSERT INTO `sys_category` VALUES ('1230769547519234050', '1230769470889299970', 'A区', NULL, 'admin', '2020-02-21 16:21:41', NULL, NULL, 'A01A03', NULL, 0);
INSERT INTO `sys_category` VALUES ('1230769620021972993', '1230769470889299970', 'B区', NULL, 'admin', '2020-02-21 16:21:58', NULL, NULL, 'A01A03', NULL, 0);
INSERT INTO `sys_category` VALUES ('1230769769930592257', '1230769253267836929', '淮南市', NULL, 'admin', '2020-02-21 16:22:34', 'admin', '2020-02-21 16:22:54', 'A01A03', '1', 0);
INSERT INTO `sys_category` VALUES ('1230769855347593217', '1230769769930592257', 'C区', NULL, 'admin', '2020-02-21 16:22:54', NULL, NULL, 'A01A03', NULL, 0);
INSERT INTO `sys_category` VALUES ('1590548229606047745', '0', '物料树C', 'C02', 'admin', '2022-11-10 11:33:44', NULL, NULL, 'A01', '1', 0);
INSERT INTO `sys_category` VALUES ('1590548229652185090', '1590548229606047745', '上衣C', 'C02A01', 'admin', '2022-11-10 11:33:44', NULL, NULL, 'A01', '1', 0);
INSERT INTO `sys_category` VALUES ('1590548229668962305', '1590548229606047745', '裤子C', 'C02A02', 'admin', '2022-11-10 11:33:44', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('1590548229685739522', '1590548229652185090', '秋衣C', 'C02A01A01', 'admin', '2022-11-10 11:33:44', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('22a50b413c5e1ef661fb8aea9469cf52', 'e9ded10fd33e5753face506f4f1564b5', 'MacBook', 'B01-2-1', 'admin', '2019-06-10 15:43:13', NULL, NULL, 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('5c8f68845e57f68ab93a2c8d82d26ae1', '0', '笔记本', 'B01', 'admin', '2019-06-10 15:34:11', 'admin', '2019-06-10 15:34:24', 'A01', '1', 0);
INSERT INTO `sys_category` VALUES ('937fd2e9aa13b8bab1da1ca36d3fd344', 'e9ded10fd33e5753face506f4f1564b5', '台式机', 'B01-2-2', 'admin', '2019-06-10 15:43:32', 'admin', '2019-08-21 12:01:59', 'A01', NULL, 0);
INSERT INTO `sys_category` VALUES ('e9ded10fd33e5753face506f4f1564b5', '5c8f68845e57f68ab93a2c8d82d26ae1', '苹果电脑', 'B01-2', 'admin', '2019-06-10 15:41:14', 'admin', '2019-06-10 15:43:13', 'A01', '1', 0);
INSERT INTO `sys_category` VALUES ('f39a06bf9f390ba4a53d11bc4e0018d7', '5c8f68845e57f68ab93a2c8d82d26ae1', '华为', 'B01-1', 'admin', '2019-06-10 15:34:24', 'admin', '2019-08-21 12:01:56', 'A01', NULL, 0);

-- ----------------------------
-- Table structure for sys_check_rule
-- ----------------------------
DROP TABLE IF EXISTS `sys_check_rule`;
CREATE TABLE `sys_check_rule`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则名称',
  `rule_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则Code',
  `rule_json` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则JSON',
  `rule_description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则描述',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scr_rule_code`(`rule_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_check_rule
-- ----------------------------
INSERT INTO `sys_check_rule` VALUES ('1224980593992388610', '通用编码规则', 'common', '[{\"digits\":1,\"pattern\":\"^[a-z|A-Z]$\",\"message\":\"第一位只能是字母\"},{\"digits\":\"*\",\"pattern\":\"^[0-9|a-z|A-Z|_]{0,}$\",\"message\":\"只能填写数字、大小写字母、下划线\"},{\"digits\":\"*\",\"pattern\":\"^.{3,}$\",\"message\":\"最少输入3位数\"},{\"digits\":\"*\",\"pattern\":\"^.{3,12}$\",\"message\":\"最多输入12位数\"}]', '规则：1、首位只能是字母；2、只能填写数字、大小写字母、下划线；3、最少3位数，最多12位数。', 'admin', '2025-09-13 18:54:19', 'admin', '2020-02-05 16:58:27');
INSERT INTO `sys_check_rule` VALUES ('1225001845524004866', '负责的功能测试', 'test', '[{\"digits\":\"*\",\"pattern\":\"^.{3,12}$\",\"message\":\"只能输入3-12位字符\"},{\"digits\":\"3\",\"pattern\":\"^\\\\d{3}$\",\"message\":\"前3位必须是数字\"},{\"digits\":\"*\",\"pattern\":\"^[^pP]*$\",\"message\":\"不能输入P\"},{\"digits\":\"4\",\"pattern\":\"^@{4}$\",\"message\":\"第4-7位必须都为 @\"},{\"digits\":\"2\",\"pattern\":\"^#=$\",\"message\":\"第8-9位必须是 #=\"},{\"digits\":\"1\",\"pattern\":\"^O$\",\"message\":\"第10位必须为大写的O\"},{\"digits\":\"*\",\"pattern\":\"^.*。$\",\"message\":\"必须以。结尾\"}]', '包含长度校验、特殊字符校验等', 'admin', '2020-02-07 11:57:31', 'admin', '2020-02-05 18:22:54');

-- ----------------------------
-- Table structure for sys_comment
-- ----------------------------
DROP TABLE IF EXISTS `sys_comment`;
CREATE TABLE `sys_comment`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `table_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '表名',
  `table_data_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据id',
  `from_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '来源用户id',
  `to_user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '发送给用户id(允许为空)',
  `comment_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '评论id(允许为空，不为空时，则为回复)',
  `comment_content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '回复内容',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_data_id`(`table_name` ASC, `table_data_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统评论回复表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_comment
-- ----------------------------
INSERT INTO `sys_comment` VALUES ('1580814554312093698', 'v3_hello', '1580529718871674882', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '1212\n:open_mouth:', 'admin', '2022-10-14 14:55:35', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1580814573433925634', 'v3_hello', '1580529718871674882', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '上传了附件', 'admin', '2022-10-14 14:55:39', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1580814621358043137', 'v3_hello', '1580529718871674882', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '干啥啊', 'admin', '2022-10-14 14:55:51', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1584490724803174402', 'v3_hello', '1580529718871674882', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '1212:nerd_face:', 'admin', '2022-10-24 18:23:22', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1584490998162743298', 'v3_hello', '1580510370266238978', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '123123', 'admin', '2022-10-24 18:24:27', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1584491122888761345', 'v3_hello', '1580510370266238978', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '333', 'admin', '2022-10-24 18:24:57', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1584493914588143617', 'v3_hello', '1580529718871674882', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '2222', 'admin', '2022-10-24 18:36:02', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1584493923496845313', 'v3_hello', '1580529718871674882', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '333', 'admin', '2022-10-24 18:36:04', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1584493984364584961', 'v3_hello', '1580510370266238978', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '上传了附件', 'admin', '2022-10-24 18:36:19', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1714455459606024193', 'ceshi_note', '1586278360710615042', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '上传了附件', 'admin', '2023-10-18 09:36:49', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1714455471815643138', 'ceshi_note', '1586278360710615042', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '121', 'admin', '2023-10-18 09:36:52', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1765261100976934914', 'ceshi_note', '1737728721647525890', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '挺好的', 'admin', '2024-03-06 14:20:18', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1765261127610765313', 'ceshi_note', '1737728721647525890', 'e9ca23d68d884d4ebb19d07889727dae', '', '', ':woozy_face:', 'admin', '2024-03-06 14:20:24', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1800557341876895745', 'ceshi_aaa', '1782647168684478466', 'e9ca23d68d884d4ebb19d07889727dae', '', '', ':face_with_head_bandage::nauseated_face:', 'admin', '2024-06-11 23:54:58', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1800557929826041858', 'ceshi_aaa', '1782647168684478466', 'e9ca23d68d884d4ebb19d07889727dae', '', '', ':cold_face:', 'admin', '2024-06-11 23:57:18', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1800557935693873154', 'ceshi_aaa', '1782647168684478466', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '1212', 'admin', '2024-06-11 23:57:19', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1800557955415490562', 'ceshi_aaa', '1782647168684478466', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '上传了附件', 'admin', '2024-06-11 23:57:24', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1800558013942808578', 'ceshi_aaa', '1782647168684478466', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '上传了附件', 'admin', '2024-06-11 23:57:38', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1805421586141544450', 'aa_order', '1805421421888405506', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '写的不错', 'admin', '2024-06-25 10:03:44', NULL, NULL);
INSERT INTO `sys_comment` VALUES ('1805421721126830082', 'aa_order', '1805421421888405506', 'e9ca23d68d884d4ebb19d07889727dae', '', '', '上传了附件', 'admin', '2024-06-25 10:04:16', NULL, NULL);

-- ----------------------------
-- Table structure for sys_data_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_data_log`;
CREATE TABLE `sys_data_log`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'id',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人真实名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `data_table` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '表名',
  `data_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据ID',
  `data_content` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '数据内容',
  `data_version` int NULL DEFAULT NULL COMMENT '版本号',
  `type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT 'json' COMMENT '类型',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sdl_data_table_id`(`data_table` ASC, `data_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_data_log
-- ----------------------------
INSERT INTO `sys_data_log` VALUES ('1942113821121011714', 'admin', '管理员', '2025-07-07 14:49:52', NULL, NULL, 'test_order_main', '1833472350097121281', '子表[订单产品明细]：修改了1条数据', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1943500714282205185', NULL, NULL, '2025-07-11 10:40:53', NULL, NULL, 'test_note', '1943500714139598850', ' 创建了记录', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1950132464605356035', 'admin', '管理员', '2025-07-29 17:53:06', NULL, NULL, 'test_note', '1943500714139598850', '  将名称为【地区】的字段内容 空 修改为 140311；    将名称为【用户名】的字段内容 ceshi 修改为 zhangsan；    将名称为【请假原因】的字段内容 空 修改为 <p>经过上述讨论, 我们不得不面对一个非常尴尬的事实, 那就是, 这样看来, 一般来讲, 我们都必须务必慎重的考虑考虑. 就我个人来说, 随机一段废话对我的意义, 不能不说非常重大.&nbsp;<br>每个人都不得不面对这些问题. 在面对这种问题时, 随机一段废话, 发生了会如何, 不发生又会如何. 总结的来说,&nbsp;<br>这种事实对本人来说意义重大, 相信对这个世界也是有一定意义的.所谓随机一段废话, 关键是x需要</p>；    将名称为【生日】的字段内容 空 修改为 2025-07-29；    将名称为【性别】的字段内容 空 修改为 空；    将名称为【年龄】的字段内容 11 修改为 0', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1950132495949389825', 'admin', '管理员', '2025-07-29 17:53:13', NULL, NULL, 'test_note', '1943500714139598850', '  将名称为【用户名】的字段内容 zhangsan 修改为 admin', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1950375804898873345', 'admin', '管理员', '2025-07-30 10:00:03', NULL, NULL, 'test_order_product', '1732300515406647298', '  将名称为【价格】的字段内容 3.0 修改为 3000.；    将名称为【产品类型】的字段内容 空 修改为 空；    将名称为【数量】的字段内容 3 修改为 10；    将名称为【产品名字】的字段内容 3 修改为 苹果手机', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1950801683948924929', NULL, NULL, '2025-07-31 14:12:20', NULL, NULL, 'test_note', '1943500714139598850', '  将名称为【年龄】的字段内容 0 修改为 1212', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1950801716647718913', NULL, NULL, '2025-07-31 14:12:28', NULL, NULL, 'test_note', '1923203898831777793', '  将名称为【用户名】的字段内容 admin 修改为 jeecg', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1966815580124278785', 'admin', '管理员', '2025-09-13 18:45:51', NULL, NULL, 'test_note', '1966815579977478145', ' 创建了记录', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1966815600902860801', 'admin', '管理员', '2025-09-13 18:45:56', NULL, NULL, 'test_note', '1966815579977478145', '  将名称为【年龄】的字段内容 0 修改为 11', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1993341836390940673', NULL, 'admin', '2025-11-25 23:31:43', NULL, NULL, 'aaa_01', '1993341836277694465', ' 创建了记录', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1993886566388453377', NULL, 'admin', '2025-11-27 11:36:17', NULL, NULL, 'test_note', '1993886566258429953', ' 创建了记录', 1, 'comment');
INSERT INTO `sys_data_log` VALUES ('1993988133426835457', NULL, 'admin', '2025-11-27 18:19:52', NULL, NULL, 'test_order_main', '1833472350097121281', '子表[订单产品明细]：修改了1条数据', 1, 'comment');

-- ----------------------------
-- Table structure for sys_data_source
-- ----------------------------
DROP TABLE IF EXISTS `sys_data_source`;
CREATE TABLE `sys_data_source`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源名称',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `db_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库类型',
  `db_driver` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '驱动类',
  `db_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源地址',
  `db_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库名称',
  `db_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `db_password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属部门',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sdc_rule_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_data_source
-- ----------------------------
INSERT INTO `sys_data_source` VALUES ('1209779538310004737', 'local_mysql', '本地测试库', '本地数据库MySQL5.7', '4', 'com.mysql.cj.jdbc.Driver', 'jdbc:mysql://127.0.0.1:3306/jeecg-boot?characterEncoding=UTF-8&useUnicode=true&useSSL=false&tinyInt1isBit=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai', 'jeecg-boot', 'root', 'f5b6775e8d1749483f2320627de0e706', 'admin', '2019-12-25 18:14:53', 'admin', '2025-09-13 18:46:43', 'A01', 0);

-- ----------------------------
-- Table structure for sys_depart
-- ----------------------------
DROP TABLE IF EXISTS `sys_depart`;
CREATE TABLE `sys_depart`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'ID',
  `parent_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父机构ID',
  `depart_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '机构/部门名称',
  `depart_name_en` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `depart_name_abbr` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '缩写',
  `depart_order` int NULL DEFAULT 0 COMMENT '排序',
  `description` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `org_category` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '1' COMMENT '机构类别 1公司，2部门，3岗位，4子公司',
  `org_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '树深度层级level',
  `org_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '机构编码',
  `mobile` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '手机号',
  `fax` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '传真',
  `address` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地址',
  `memo` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `status` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态（1启用，0不启用）',
  `del_flag` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '删除状态（0，正常，1已删除）',
  `qywx_identifier` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '对接企业微信的ID',
  `ding_identifier` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '对接钉钉部门的ID',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  `iz_leaf` tinyint(1) NULL DEFAULT 0 COMMENT '是否有叶子节点: 1是0否',
  `position_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '职级id',
  `dep_post_parent_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '上级岗位id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_depart_org_code`(`org_code` ASC) USING BTREE,
  INDEX `idx_sd_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_sd_depart_order`(`depart_order` ASC) USING BTREE,
  INDEX `idx_sd_position_id`(`position_id` ASC) USING BTREE,
  INDEX `idx_sd_dep_post_parent_id`(`dep_post_parent_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '组织机构表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_depart
-- ----------------------------
INSERT INTO `sys_depart` VALUES ('1582683631414632450', 'c6d7cb4deeac411cb3384b1b31278596', '销售公关岗位', NULL, NULL, 0, NULL, '3', '2', 'A01A06', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2022-10-19 18:42:37', NULL, NULL, 0, 1, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958496243038556161', '', '控股集团', NULL, NULL, 0, NULL, '2', '1', 'A05', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:47:48', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958496444470005762', '1958496243038556161', '投资控股集团有限公司', NULL, NULL, 0, NULL, '4', '2', 'A05A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:48:34', 'admin', '2025-08-21 19:49:57', 0, 0, NULL, '');
INSERT INTO `sys_depart` VALUES ('1958496759810363394', '1958496243038556161', '城市运营管理集团有限公司', NULL, NULL, 1, NULL, '4', '2', 'A05A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:49:49', 'admin', '2025-08-21 20:30:23', 0, 0, NULL, '');
INSERT INTO `sys_depart` VALUES ('1958496836318662658', '1958496444470005762', '领导班子', NULL, NULL, 0, NULL, '2', '3', 'A05A01A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:50:08', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958496891561840641', '1958496444470005762', '办公室', NULL, NULL, 2, NULL, '2', '3', 'A05A01A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:50:21', 'admin', '2025-08-21 19:50:36', 0, 0, NULL, '');
INSERT INTO `sys_depart` VALUES ('1958496943017562114', '1958496444470005762', '财务管理中心', NULL, NULL, 3, NULL, '2', '3', 'A05A01A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:50:33', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958497020528300033', '1958496444470005762', '投资发展部', NULL, NULL, 4, NULL, '2', '3', 'A05A01A04', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:50:51', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958497164103520258', '1958496836318662658', '董事长', NULL, NULL, 0, NULL, '3', '4', 'A05A01A01A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:51:26', NULL, NULL, 0, 1, '1958471111989067778', '');
INSERT INTO `sys_depart` VALUES ('1958497256772472834', '1958496836318662658', '党委书记', NULL, NULL, 1, NULL, '3', '4', 'A05A01A01A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:51:48', 'admin', '2025-08-21 19:54:53', 0, 1, '1958471030867034113', '1958497164103520258');
INSERT INTO `sys_depart` VALUES ('1958497591230468098', '1958496836318662658', '控股总经理', NULL, NULL, 3, NULL, '3', '4', 'A05A01A01A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:53:08', 'admin', '2025-08-21 19:54:42', 0, 1, '1958471074953363458', '1958497164103520258');
INSERT INTO `sys_depart` VALUES ('1958497769387724802', '1958496836318662658', '纪委书记', NULL, NULL, 4, NULL, '3', '4', 'A05A01A01A04', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:53:50', 'admin', '2025-08-21 19:54:06', 0, 1, '1958471030867034113', '1958497591230468098');
INSERT INTO `sys_depart` VALUES ('1958498187287203841', '1958496891561840641', '控股办公室主任', NULL, NULL, 1, NULL, '3', '4', 'A05A01A02A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:55:30', 'admin', '2025-08-21 19:55:50', 0, 1, '1958470912214368258', '1958497164103520258');
INSERT INTO `sys_depart` VALUES ('1958498716927135745', '1958496891561840641', '副主任', NULL, NULL, 2, NULL, '3', '4', 'A05A01A02A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 19:57:36', 'admin', '2025-08-21 19:57:50', 0, 1, '1958470865577902082', '1958498187287203841');
INSERT INTO `sys_depart` VALUES ('1958502219078733826', '1958496891561840641', '职员', NULL, NULL, 3, NULL, '3', '4', 'A05A01A02A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:11:31', NULL, NULL, 0, 1, '1958470823064436737', '1958498187287203841');
INSERT INTO `sys_depart` VALUES ('1958502381062754305', '1958496943017562114', '主任', NULL, NULL, 1, NULL, '3', '4', 'A05A01A03A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:12:10', 'admin', '2025-08-21 20:13:13', 0, 1, '1958470912214368258', '1958502611426512898');
INSERT INTO `sys_depart` VALUES ('1958502611426512898', '1958496836318662658', '控股副总经理', NULL, NULL, 5, NULL, '3', '4', 'A05A01A01A05', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:13:04', 'admin', '2025-08-21 20:27:14', 0, 1, '1958471030867034113', '1958497591230468098');
INSERT INTO `sys_depart` VALUES ('1958502810966331393', '1958496943017562114', '副主任', NULL, NULL, 2, NULL, '3', '4', 'A05A01A03A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:13:52', 'admin', '2025-08-21 20:14:40', 0, 1, '1958470865577902082', '1958502381062754305');
INSERT INTO `sys_depart` VALUES ('1958502942289989634', '1958496943017562114', '职员', NULL, NULL, 2, NULL, '3', '4', 'A05A01A03A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:14:23', 'admin', '2025-08-21 20:14:28', 0, 1, '1958470823064436737', '1958502810966331393');
INSERT INTO `sys_depart` VALUES ('1958503159999533057', '1958497020528300033', '部长', NULL, NULL, 1, NULL, '3', '4', 'A05A01A04A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:15:15', NULL, NULL, 0, 1, '1958470912214368258', '1958502611426512898');
INSERT INTO `sys_depart` VALUES ('1958503409933914114', '1958497020528300033', '副部长', NULL, NULL, 2, NULL, '3', '4', 'A05A01A04A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:16:15', NULL, NULL, 0, 1, '1958470865577902082', '1958503159999533057');
INSERT INTO `sys_depart` VALUES ('1958503468805165058', '1958497020528300033', '员工', NULL, NULL, 3, NULL, '3', '4', 'A05A01A04A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:16:29', NULL, NULL, 0, 1, '1958470823064436737', '1958503409933914114');
INSERT INTO `sys_depart` VALUES ('1958507136782733313', '1958496759810363394', '领导班子', NULL, NULL, 1, NULL, '2', '3', 'A05A02A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:31:03', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958507356253884418', '1958496759810363394', '信息技术发展有限公司', NULL, NULL, 4, NULL, '4', '3', 'A05A02A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:31:56', 'admin', '2025-08-21 21:12:57', 0, 0, NULL, '');
INSERT INTO `sys_depart` VALUES ('1958507448138502146', '1958507136782733313', '董事长', NULL, NULL, 1, NULL, '3', '4', 'A05A02A01A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:32:18', 'admin', '2025-08-21 20:33:24', 0, 1, '1958471111989067778', '');
INSERT INTO `sys_depart` VALUES ('1958507542866857985', '1958507136782733313', '副总经理', NULL, NULL, 3, NULL, '3', '4', 'A05A02A01A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:32:40', 'admin', '2025-08-21 20:33:26', 0, 1, '1958471030867034113', '');
INSERT INTO `sys_depart` VALUES ('1958507650828242946', '1958507136782733313', '总经理', NULL, NULL, 2, NULL, '3', '4', 'A05A02A01A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 20:33:06', 'admin', '2025-08-21 20:33:20', 0, 1, '1958471074953363458', '');
INSERT INTO `sys_depart` VALUES ('1958518943542972418', '1958507356253884418', '领导班子', NULL, NULL, 1, NULL, '2', '4', 'A05A02A03A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:17:58', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958519010207240193', '1958507356253884418', '综合管理部', NULL, NULL, 2, NULL, '2', '4', 'A05A02A03A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:18:14', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958519045623943169', '1958507356253884418', '财务部', NULL, NULL, 3, NULL, '2', '4', 'A05A02A03A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:18:23', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958519097058693121', '1958507356253884418', '软件研发部', NULL, NULL, 4, NULL, '2', '4', 'A05A02A03A04', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:18:35', NULL, NULL, 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('1958520331257810945', '1958496891561840641', '总工程师', NULL, NULL, 2, NULL, '3', '4', 'A05A01A02A04', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:23:29', 'admin', '2025-08-21 21:23:52', 0, 1, '1958471030867034113', '1958497164103520258');
INSERT INTO `sys_depart` VALUES ('1958520788395003906', '1958496759810363394', '办公室', NULL, NULL, 2, NULL, '2', '3', 'A05A02A04', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:25:18', 'admin', '2025-08-21 21:26:51', 0, 0, NULL, '');
INSERT INTO `sys_depart` VALUES ('1958520876810932225', '1958520788395003906', '总工程师', NULL, NULL, 1, NULL, '3', '4', 'A05A02A04A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:25:39', 'admin', '2025-08-21 21:26:25', 0, 1, '1958471030867034113', '1958507650828242946');
INSERT INTO `sys_depart` VALUES ('1958521034948775937', '1958518943542972418', '执行董事兼总经理', NULL, NULL, 1, NULL, '3', '5', 'A05A02A03A01A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:26:17', NULL, NULL, 0, 1, '1958471111989067778', '1958520876810932225');
INSERT INTO `sys_depart` VALUES ('1958521513564999681', '1958518943542972418', '副总经理', NULL, NULL, 2, NULL, '3', '5', 'A05A02A03A01A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:28:11', NULL, NULL, 0, 1, '1958471030867034113', '1958521034948775937');
INSERT INTO `sys_depart` VALUES ('1958521634549698561', '1958519010207240193', '副部长', NULL, NULL, 1, NULL, '3', '5', 'A05A02A03A02A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:28:40', 'admin', '2025-08-21 21:30:15', 0, 1, '1958470865577902082', '1958521034948775937');
INSERT INTO `sys_depart` VALUES ('1958523766002716674', '1958519010207240193', '信息归档员', NULL, NULL, 2, NULL, '3', '5', 'A05A02A03A02A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:37:08', NULL, NULL, 0, 1, '1958470823064436737', '1958521634549698561');
INSERT INTO `sys_depart` VALUES ('1958524282631917570', '1958519045623943169', '部长', NULL, NULL, 1, NULL, '3', '5', 'A05A02A03A03A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:39:11', NULL, NULL, 0, 1, '1958470912214368258', '1958521034948775937');
INSERT INTO `sys_depart` VALUES ('1958524369311404033', '1958519045623943169', '出纳', NULL, NULL, 2, NULL, '3', '5', 'A05A02A03A03A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:39:32', NULL, NULL, 0, 1, '1958470823064436737', '1958524282631917570');
INSERT INTO `sys_depart` VALUES ('1958524471841165313', '1958519097058693121', '项目经理', NULL, NULL, 1, NULL, '3', '5', 'A05A02A03A04A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:39:56', NULL, NULL, 0, 1, '1958470912214368258', '1958521513564999681');
INSERT INTO `sys_depart` VALUES ('1958524565596442626', '1958519097058693121', '软件工程师', NULL, NULL, 2, NULL, '3', '5', 'A05A02A03A04A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2025-08-21 21:40:19', NULL, NULL, 0, 1, '1958470823064436737', '1958524471841165313');
INSERT INTO `sys_depart` VALUES ('4f1765520d6346f9bd9c79e2479e5b12', 'c6d7cb4deeac411cb3384b1b31278596', '市场部', NULL, NULL, 0, NULL, '2', '2', 'A01A03', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2019-02-20 17:15:34', 'admin', '2019-02-26 16:36:18', 0, 1, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('5159cde220114246b045e574adceafe9', '6d35e179cd814e3299bd588ea7daed3f', '研发部', NULL, NULL, 0, NULL, '2', '2', 'A02A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2019-02-26 16:44:38', 'admin', '2019-03-07 09:36:53', 0, 1, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('57197590443c44f083d42ae24ef26a2c', 'c6d7cb4deeac411cb3384b1b31278596', '研发部', NULL, NULL, 0, NULL, '2', '2', 'A01A05', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2019-02-21 16:14:41', 'admin', '2019-03-27 19:05:49', 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('63775228b7b041a99825f79760590b7d', '57197590443c44f083d42ae24ef26a2c', '研发经理', NULL, NULL, 0, NULL, '3', '3', 'A01A05A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2020-05-02 15:29:09', NULL, NULL, 0, 1, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('67fc001af12a4f9b8458005d3f19934a', 'c6d7cb4deeac411cb3384b1b31278596', '财务部', NULL, NULL, 0, NULL, '2', '2', 'A01A04', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2019-02-21 16:14:35', 'admin', '2019-02-25 12:49:41', 0, 1, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('6d35e179cd814e3299bd588ea7daed3f', '', '北京卓尔互动', NULL, NULL, 0, NULL, '2', '1', 'A02', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2019-02-26 16:36:39', 'admin', '2020-05-02 18:21:22', 0, 0, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('a7d7e77e06c84325a40932163adcdaa6', '6d35e179cd814e3299bd588ea7daed3f', '财务部', NULL, NULL, 0, NULL, '2', '2', 'A02A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2019-02-26 16:36:47', 'admin', '2022-10-14 18:57:56', 0, 1, NULL, NULL);
INSERT INTO `sys_depart` VALUES ('c6d7cb4deeac411cb3384b1b31278596', '', '北京国炬软件', NULL, NULL, 0, NULL, '2', '1', 'A01', NULL, NULL, NULL, NULL, NULL, '0', NULL, NULL, 'admin', '2019-02-11 14:21:51', 'admin', '2020-05-02 18:21:27', 0, 0, NULL, NULL);

-- ----------------------------
-- Table structure for sys_depart_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_depart_permission`;
CREATE TABLE `sys_depart_permission`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `depart_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '部门id',
  `permission_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限id',
  `data_rule_ids` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据规则id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '部门权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_depart_permission
-- ----------------------------
INSERT INTO `sys_depart_permission` VALUES ('1260925131934519297', '6d35e179cd814e3299bd588ea7daed3f', 'f0675b52d89100ee88472b6800754a08', NULL);
INSERT INTO `sys_depart_permission` VALUES ('1260925131947102209', '6d35e179cd814e3299bd588ea7daed3f', '2aeddae571695cd6380f6d6d334d6e7d', NULL);
INSERT INTO `sys_depart_permission` VALUES ('1260925131955490818', '6d35e179cd814e3299bd588ea7daed3f', '020b06793e4de2eee0007f603000c769', NULL);
INSERT INTO `sys_depart_permission` VALUES ('1260925131959685121', '6d35e179cd814e3299bd588ea7daed3f', '1232123780958064642', NULL);
INSERT INTO `sys_depart_permission` VALUES ('1694946354772217858', '1582683631414632450', '1588513553652436993', NULL);
INSERT INTO `sys_depart_permission` VALUES ('1694946354784800769', '1582683631414632450', '1592114574275211265', NULL);

-- ----------------------------
-- Table structure for sys_depart_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_depart_role`;
CREATE TABLE `sys_depart_role`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `depart_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '部门id',
  `role_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '部门角色名称',
  `role_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '部门角色编码',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '部门角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_depart_role
-- ----------------------------
INSERT INTO `sys_depart_role` VALUES ('1260925293226479618', '6d35e179cd814e3299bd588ea7daed3f', 'roless', 'ssss', NULL, 'admin', '2020-05-14 21:29:51', NULL, NULL);

-- ----------------------------
-- Table structure for sys_depart_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_depart_role_permission`;
CREATE TABLE `sys_depart_role_permission`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `depart_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '部门id',
  `role_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色id',
  `permission_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限id',
  `data_rule_ids` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据权限ids',
  `operate_date` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `operate_ip` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '操作ip',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sdrp_role_per_id`(`role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `idx_sdrp_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_sdrp_per_id`(`permission_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '部门角色权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_depart_role_permission
-- ----------------------------
INSERT INTO `sys_depart_role_permission` VALUES ('1260925328689319938', NULL, '1260925293226479618', '2aeddae571695cd6380f6d6d334d6e7d', NULL, NULL, NULL);
INSERT INTO `sys_depart_role_permission` VALUES ('1260925328706097153', NULL, '1260925293226479618', '020b06793e4de2eee0007f603000c769', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for sys_depart_role_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_depart_role_user`;
CREATE TABLE `sys_depart_role_user`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `drole_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sdr_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_sdr_role_id`(`drole_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '部门角色用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_depart_role_user
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dict
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `dict_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典名称',
  `dict_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典编码',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `del_flag` int NULL DEFAULT NULL COMMENT '删除状态',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `type` int(1) UNSIGNED ZEROFILL NULL DEFAULT 0 COMMENT '字典类型0为string,1为number',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  `low_app_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '低代码应用ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sd_dict_code`(`dict_code` ASC) USING BTREE,
  INDEX `uk_sd_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES ('0b5d19e1fce4b2e6647e6b4a17760c14', '通告类型', 'msg_category', '消息类型1:通知公告2:系统消息', 0, 'admin', '2019-04-22 18:01:35', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1174509082208395266', '职务职级', 'position_rank', '职务表职级字典', 0, 'admin', '2019-09-19 10:22:41', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1174511106530525185', '机构类型', 'org_category', '机构类型 1公司，2部门，3岗位，4子公司', 0, 'admin', '2019-09-19 10:30:43', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1178295274528845826', '表单权限策略', 'form_perms_type', '', 0, 'admin', '2019-09-29 21:07:39', 'admin', '2019-09-29 21:08:26', NULL, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1199517671259906049', '紧急程度', 'urgent_level', '日程计划紧急程度', 0, 'admin', '2019-11-27 10:37:53', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1199518099888414722', '日程计划类型', 'eoa_plan_type', '', 0, 'admin', '2019-11-27 10:39:36', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1199520177767587841', '分类栏目类型', 'eoa_cms_menu_type', '', 0, 'admin', '2019-11-27 10:47:51', 'admin', '2019-11-27 10:49:35', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1199525215290306561', '日程计划状态', 'eoa_plan_status', '', 0, 'admin', '2019-11-27 11:07:52', 'admin', '2019-11-27 11:10:11', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1209733563293962241', '数据库类型', 'database_type', '', 0, 'admin', '2019-12-25 15:12:12', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1232913193820581889', 'Online表单业务分类', 'ol_form_biz_type', '', 0, 'admin', '2020-02-27 14:19:46', 'admin', '2020-02-27 14:20:23', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1242298510024429569', '提醒方式', 'remindMode', '', 0, 'admin', '2020-03-24 11:53:40', 'admin', '2020-03-24 12:03:22', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1250687930947620866', '定时任务状态', 'quartz_status', '', 0, 'admin', '2020-04-16 15:30:14', '', NULL, NULL, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1280401766745718786', '租户状态', 'tenant_status', '租户状态', 0, 'admin', '2020-07-07 15:22:25', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1356445645198135298', '开关', 'is_open', '', 0, 'admin', '2021-02-02 11:33:38', 'admin', '2021-02-02 15:28:12', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1600042215909134338', '所属行业', 'trade', '行业', 0, 'admin', '2022-12-06 16:19:26', 'admin', '2022-12-06 16:20:50', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1600044537800331266', '公司规模', 'company_size', '公司规模', 0, 'admin', '2022-12-06 16:28:40', 'admin', '2022-12-06 16:30:23', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1606645341269299201', '职级', 'company_rank', '公司职级', 0, 'admin', '2022-12-24 21:37:54', 'admin', '2022-12-24 21:38:25', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1606646440684457986', '公司部门', 'company_department', '公司部门', 0, 'admin', '2022-12-24 21:42:16', 'admin', '2024-03-18 14:21:56', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1693196536609755137', 'ddd', 'ddd', NULL, 1, 'admin', '2023-08-20 17:41:27', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1784843187992084482', '客户终端类型', 'client_type', NULL, 0, 'jeecg', '2024-04-29 15:12:31', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1890229208685322242', '模型提供者', 'model_provider', NULL, 0, 'jeecg', '2025-02-14 10:38:57', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1891456510739890177', '模型类型', 'model_type', NULL, 0, 'jeecg', '2025-02-17 19:55:48', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1891671216561975297', '知识库类型', 'airag_know_type', NULL, 1, 'jeecg', '2025-02-18 10:08:58', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1891672414555860993', '知识库文档类型', 'know_doc_type', NULL, 0, 'jeecg', '2025-02-18 10:13:44', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1894701158027554818', 'AI应用类型', 'ai_app_type', NULL, 0, 'jeecg', '2025-02-26 18:48:53', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1934846825077878786', '公告分类', 'notice_type', NULL, 0, 'admin', '2025-06-17 13:33:25', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1937393911539384322', '模版分类', 'msgCategory', NULL, 0, 'admin', '2025-06-24 14:14:38', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1939572486447292418', '首页关联', 'relation_type', NULL, 0, 'admin', '2025-06-30 14:31:31', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('1964944899916697602', '用户职务', 'user_position', '用户职务', 0, 'admin', '2025-09-08 14:52:26', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('236e8a4baff0db8c62c00dd95632834f', '同步工作流引擎', 'activiti_sync', '同步工作流引擎', 0, 'admin', '2019-05-15 15:27:33', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('2e02df51611a4b9632828ab7e5338f00', '权限策略', 'perms_type', '权限策略', 0, 'admin', '2019-04-26 18:26:55', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('2f0320997ade5dd147c90130f7218c3e', '推送类别', 'msg_type', '', 0, 'admin', '2019-03-17 21:21:32', 'admin', '2019-03-26 19:57:45', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('3486f32803bb953e7155dab3513dc68b', '删除状态', 'del_flag', NULL, 0, 'admin', '2019-01-18 21:46:26', 'admin', '2019-03-30 11:17:11', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('3d9a351be3436fbefb1307d4cfb49bf2', '性别', 'sex', NULL, 0, NULL, '2019-01-04 14:56:32', 'admin', '2019-03-30 11:28:27', 1, 0, NULL);
INSERT INTO `sys_dict` VALUES ('4274efc2292239b6f000b153f50823ff', '全局权限策略', 'global_perms_type', '全局权限策略', 0, 'admin', '2019-05-10 17:54:05', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('4c03fca6bf1f0299c381213961566349', 'Online图表展示模板', 'online_graph_display_template', 'Online图表展示模板', 0, 'admin', '2019-04-12 17:28:50', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('4c753b5293304e7a445fd2741b46529d', '字典状态', 'dict_item_status', NULL, 0, 'admin', '2020-06-18 23:18:42', 'admin', '2019-03-30 19:33:52', 1, 0, NULL);
INSERT INTO `sys_dict` VALUES ('4d7fec1a7799a436d26d02325eff295e', '优先级', 'priority', '优先级', 0, 'admin', '2019-03-16 17:03:34', 'admin', '2019-04-16 17:39:23', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('4e4602b3e3686f0911384e188dc7efb4', '条件规则', 'rule_conditions', '', 0, 'admin', '2019-04-01 10:15:03', 'admin', '2019-04-01 10:30:47', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('4f69be5f507accea8d5df5f11346181a', '发送消息类型', 'msgType', NULL, 0, 'admin', '2019-04-11 14:27:09', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('68168534ff5065a152bfab275c2136f8', '有效无效状态', 'valid_status', '有效无效状态', 0, 'admin', '2020-09-26 19:21:14', 'admin', '2019-04-26 19:21:23', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('6b78e3f59faec1a4750acff08030a79b', '用户类型', 'user_type', NULL, 0, NULL, '2019-01-04 14:59:01', 'admin', '2019-03-18 23:28:18', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('72cce0989df68887546746d8f09811aa', 'Online表单类型', 'cgform_table_type', '', 0, 'admin', '2019-01-27 10:13:02', 'admin', '2019-03-30 11:37:36', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('78bda155fe380b1b3f175f1e88c284c6', '流程状态', 'bpm_status', '流程状态', 0, 'admin', '2019-05-09 16:31:52', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('83bfb33147013cc81640d5fd9eda030c', '日志类型', 'log_type', NULL, 0, 'admin', '2019-03-18 23:22:19', NULL, NULL, 1, 0, NULL);
INSERT INTO `sys_dict` VALUES ('845da5006c97754728bf48b6a10f79cc', '状态', 'status', NULL, 0, 'admin', '2019-03-18 21:45:25', 'admin', '2019-03-18 21:58:25', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('880a895c98afeca9d9ac39f29e67c13e', '操作类型', 'operate_type', '操作类型', 0, 'admin', '2019-07-22 10:54:29', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('8dfe32e2d29ea9430a988b3b558bf233', '发布状态', 'send_status', '发布状态', 0, 'admin', '2019-04-16 17:40:42', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('a7adbcd86c37f7dbc9b66945c82ef9e6', '1是0否', 'yn', '', 0, 'admin', '2019-05-22 19:29:29', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('a9d9942bd0eccb6e89de92d130ec4c4a', '消息发送状态', 'msgSendStatus', NULL, 0, 'admin', '2019-04-12 18:18:17', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('ac2f7c0c5c5775fcea7e2387bcb22f01', '菜单类型', 'menu_type', NULL, 0, 'admin', '2020-12-18 23:24:32', 'admin', '2019-04-01 15:27:06', 1, 0, NULL);
INSERT INTO `sys_dict` VALUES ('ad7c65ba97c20a6805d5dcdf13cdaf36', 'onlineT类型', 'ceshi_online', NULL, 0, 'admin', '2019-03-22 16:31:49', 'admin', '2019-03-22 16:34:16', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('bd1b8bc28e65d6feefefb6f3c79f42fd', 'Online图表数据类型', 'online_graph_data_type', 'Online图表数据类型', 0, 'admin', '2019-04-12 17:24:24', 'admin', '2019-04-12 17:24:57', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('c36169beb12de8a71c8683ee7c28a503', '部门状态', 'depart_status', NULL, 0, 'admin', '2019-03-18 21:59:51', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('c5a14c75172783d72cbee6ee7f5df5d1', 'Online图表类型', 'online_graph_type', 'Online图表类型', 0, 'admin', '2019-04-12 17:04:06', NULL, NULL, 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('d6e1152968b02d69ff358c75b48a6ee1', '流程类型', 'bpm_process_type', NULL, 0, 'admin', '2021-02-22 19:26:54', 'admin', '2019-03-30 18:14:44', 0, 0, NULL);
INSERT INTO `sys_dict` VALUES ('fc6cd58fde2e8481db10d3a1e68ce70c', '用户状态', 'user_status', NULL, 0, 'admin', '2019-03-18 21:57:25', 'admin', '2019-03-18 23:11:58', 1, 0, NULL);

-- ----------------------------
-- Table structure for sys_dict_item
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_item`;
CREATE TABLE `sys_dict_item`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `dict_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典id',
  `item_text` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典项文本',
  `item_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典项值',
  `item_color` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典项颜色',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `sort_order` int NULL DEFAULT NULL COMMENT '排序',
  `status` int NULL DEFAULT NULL COMMENT '状态（1启用 0不启用）',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sditem_role_dict_id`(`dict_id` ASC) USING BTREE,
  INDEX `idx_sditem_role_sort_order`(`sort_order` ASC) USING BTREE,
  INDEX `idx_sditem_status`(`status` ASC) USING BTREE,
  INDEX `idx_sditem_dict_val`(`dict_id` ASC, `item_value` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_dict_item
-- ----------------------------
INSERT INTO `sys_dict_item` VALUES ('0072d115e07c875d76c9b022e2179128', '4d7fec1a7799a436d26d02325eff295e', '低', 'L', NULL, '低', 3, 1, 'admin', '2019-04-16 17:04:59', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('05a2e732ce7b00aa52141ecc3e330b4e', '3486f32803bb953e7155dab3513dc68b', '已删除', '1', NULL, NULL, NULL, 1, 'admin', '2025-10-18 21:46:56', 'admin', '2019-03-28 22:23:20');
INSERT INTO `sys_dict_item` VALUES ('096c2e758d823def3855f6376bc736fb', 'bd1b8bc28e65d6feefefb6f3c79f42fd', 'SQL', 'sql', NULL, NULL, 1, 1, 'admin', '2019-04-12 17:26:26', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('0c9532916f5cd722017b46bc4d953e41', '2f0320997ade5dd147c90130f7218c3e', '指定用户', 'USER', NULL, NULL, NULL, 1, 'admin', '2019-03-17 21:22:19', 'admin', '2019-03-17 21:22:28');
INSERT INTO `sys_dict_item` VALUES ('0ca4beba9efc4f9dd54af0911a946d5c', '72cce0989df68887546746d8f09811aa', '附表', '3', NULL, NULL, 3, 1, 'admin', '2019-03-27 10:13:43', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1030a2652608f5eac3b49d70458b8532', '2e02df51611a4b9632828ab7e5338f00', '禁用', '2', NULL, '禁用', 2, 1, 'admin', '2021-03-26 18:27:28', 'admin', '2019-04-26 18:39:11');
INSERT INTO `sys_dict_item` VALUES ('1174509082208395266', '1174511106530525185', '岗位', '3', NULL, '岗位', 1, 1, 'admin', '2019-09-19 10:31:16', '', NULL);
INSERT INTO `sys_dict_item` VALUES ('1174509601047994369', '1174509082208395266', '员级', '1', NULL, '', 1, 1, 'admin', '2019-09-19 10:24:45', 'admin', '2019-09-23 11:46:39');
INSERT INTO `sys_dict_item` VALUES ('1174509667297026049', '1174509082208395266', '助级', '2', NULL, '', 2, 1, 'admin', '2019-09-19 10:25:01', 'admin', '2019-09-23 11:46:47');
INSERT INTO `sys_dict_item` VALUES ('1174509713568587777', '1174509082208395266', '中级', '3', NULL, '', 3, 1, 'admin', '2019-09-19 10:25:12', 'admin', '2019-09-23 11:46:56');
INSERT INTO `sys_dict_item` VALUES ('1174509788361416705', '1174509082208395266', '副高级', '4', NULL, '', 4, 1, 'admin', '2019-09-19 10:25:30', 'admin', '2019-09-23 11:47:06');
INSERT INTO `sys_dict_item` VALUES ('1174509835803189250', '1174509082208395266', '正高级', '5', NULL, '', 5, 1, 'admin', '2019-09-19 10:25:41', 'admin', '2019-09-23 11:47:12');
INSERT INTO `sys_dict_item` VALUES ('1174511197735665665', '1174511106530525185', '公司', '1', NULL, '公司', 1, 1, 'admin', '2019-09-19 10:31:05', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1174511244036587521', '1174511106530525185', '部门', '2', NULL, '部门', 1, 1, 'admin', '2019-09-19 10:31:16', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1178295553450061826', '1178295274528845826', '可编辑(未授权禁用)', '2', NULL, '', 2, 1, 'admin', '2019-09-29 21:08:46', 'admin', '2019-09-29 21:09:18');
INSERT INTO `sys_dict_item` VALUES ('1178295639554928641', '1178295274528845826', '可见(未授权不可见)', '1', NULL, '', 1, 1, 'admin', '2019-09-29 21:09:06', 'admin', '2019-09-29 21:09:24');
INSERT INTO `sys_dict_item` VALUES ('1199517884758368257', '1199517671259906049', '一般', '1', NULL, '', 1, 1, 'admin', '2019-11-27 10:38:44', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199517914017832962', '1199517671259906049', '重要', '2', NULL, '', 1, 1, 'admin', '2019-11-27 10:38:51', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199517941339529217', '1199517671259906049', '紧急', '3', NULL, '', 1, 1, 'admin', '2019-11-27 10:38:58', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199518186144276482', '1199518099888414722', '日常记录', '1', NULL, '', 1, 1, 'admin', '2019-11-27 10:39:56', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199518214858481666', '1199518099888414722', '本周工作', '2', NULL, '', 1, 1, 'admin', '2019-11-27 10:40:03', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199518235943247874', '1199518099888414722', '下周计划', '3', NULL, '', 1, 1, 'admin', '2019-11-27 10:40:08', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199520817285701634', '1199520177767587841', '列表', '1', NULL, '', 1, 1, 'admin', '2019-11-27 10:50:24', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199520835035996161', '1199520177767587841', '链接', '2', NULL, '', 1, 1, 'admin', '2019-11-27 10:50:28', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199525468672405505', '1199525215290306561', '未开始', '0', NULL, '', 1, 1, 'admin', '2019-11-27 11:08:52', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199525490575060993', '1199525215290306561', '进行中', '1', NULL, '', 1, 1, 'admin', '2019-11-27 11:08:58', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1199525506429530114', '1199525215290306561', '已完成', '2', NULL, '', 1, 1, 'admin', '2019-11-27 11:09:02', 'admin', '2019-11-27 11:10:02');
INSERT INTO `sys_dict_item` VALUES ('1209733775114702850', '1209733563293962241', 'MySQL5.5', '1', NULL, '', 1, 1, 'admin', '2019-12-25 15:13:02', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1209733839933476865', '1209733563293962241', 'Oracle', '2', NULL, '', 3, 1, 'admin', '2019-12-25 15:13:18', 'admin', '2021-07-15 13:44:08');
INSERT INTO `sys_dict_item` VALUES ('1209733903020003330', '1209733563293962241', 'SQLServer', '3', NULL, '', 4, 1, 'admin', '2019-12-25 15:13:33', 'admin', '2021-07-15 13:44:11');
INSERT INTO `sys_dict_item` VALUES ('1232913424813486081', '1232913193820581889', '官方示例', 'demo', NULL, '', 1, 1, 'admin', '2020-02-27 14:20:42', 'admin', '2020-02-27 14:21:37');
INSERT INTO `sys_dict_item` VALUES ('1232913493717512194', '1232913193820581889', '流程表单', 'bpm', NULL, '', 2, 1, 'admin', '2020-02-27 14:20:58', 'admin', '2020-02-27 14:22:20');
INSERT INTO `sys_dict_item` VALUES ('1232913605382467585', '1232913193820581889', '测试表单', 'temp', NULL, '', 4, 1, 'admin', '2020-02-27 14:21:25', 'admin', '2020-02-27 14:22:16');
INSERT INTO `sys_dict_item` VALUES ('1232914232372195330', '1232913193820581889', '导入表单', 'bdfl_include', NULL, '', 5, 1, 'admin', '2020-02-27 14:23:54', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1234371726545010689', '4e4602b3e3686f0911384e188dc7efb4', '左模糊', 'LEFT_LIKE', NULL, '左模糊', 7, 1, 'admin', '2020-03-02 14:55:27', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1234371809495760898', '4e4602b3e3686f0911384e188dc7efb4', '右模糊', 'RIGHT_LIKE', NULL, '右模糊', 7, 1, 'admin', '2020-03-02 14:55:47', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1242300779390357505', '1242298510024429569', '短信提醒', '2', NULL, '', 2, 1, 'admin', '2020-03-24 12:02:41', 'admin', '2020-03-30 18:21:33');
INSERT INTO `sys_dict_item` VALUES ('1242300814383435777', '1242298510024429569', '邮件提醒', '1', NULL, '', 1, 1, 'admin', '2020-03-24 12:02:49', 'admin', '2020-03-30 18:21:26');
INSERT INTO `sys_dict_item` VALUES ('1242300887343353857', '1242298510024429569', '系统消息', '4', NULL, '', 4, 1, 'admin', '2020-03-24 12:03:07', 'admin', '2020-03-30 18:21:43');
INSERT INTO `sys_dict_item` VALUES ('1250688147579228161', '1250687930947620866', '正常', '0', NULL, '', 1, 1, 'admin', '2020-04-16 15:31:05', '', NULL);
INSERT INTO `sys_dict_item` VALUES ('1250688201064992770', '1250687930947620866', '停止', '-1', NULL, '', 1, 1, 'admin', '2020-04-16 15:31:18', '', NULL);
INSERT INTO `sys_dict_item` VALUES ('1280401815068295170', '1280401766745718786', '正常', '1', NULL, '', 1, 1, 'admin', '2020-07-07 15:22:36', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1280401847607705602', '1280401766745718786', '冻结', '0', NULL, '', 1, 1, 'admin', '2020-07-07 15:22:44', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1305827309355302914', 'bd1b8bc28e65d6feefefb6f3c79f42fd', 'API', 'api', NULL, '', 3, 1, 'admin', '2020-09-15 19:14:26', 'admin', '2020-09-15 19:14:41');
INSERT INTO `sys_dict_item` VALUES ('1334440962954936321', '1209733563293962241', 'MYSQL5.7+', '4', NULL, '', 2, 1, 'admin', '2020-12-03 18:16:02', 'admin', '2021-07-15 13:44:29');
INSERT INTO `sys_dict_item` VALUES ('1356445705549975553', '1356445645198135298', '是', 'Y', NULL, '', 1, 1, 'admin', '2021-02-02 11:33:52', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1356445754212290561', '1356445645198135298', '否', 'N', NULL, '', 1, 1, 'admin', '2021-02-02 11:34:04', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1414837074500976641', '1209733563293962241', 'postgresql', '6', NULL, '', 5, 1, 'admin', '2021-07-13 14:40:20', 'admin', '2021-07-15 13:44:15');
INSERT INTO `sys_dict_item` VALUES ('1415547541091504129', '1209733563293962241', 'marialDB', '5', NULL, '', 6, 1, 'admin', '2021-07-15 13:43:28', 'admin', '2021-07-15 13:44:23');
INSERT INTO `sys_dict_item` VALUES ('1418049969003089922', '1209733563293962241', '达梦', '7', NULL, '', 7, 1, 'admin', '2021-07-22 11:27:13', 'admin', '2021-07-22 11:27:30');
INSERT INTO `sys_dict_item` VALUES ('1418050017053036545', '1209733563293962241', '人大金仓', '8', NULL, '', 8, 1, 'admin', '2021-07-22 11:27:25', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1418050075555188737', '1209733563293962241', '神通', '9', NULL, '', 9, 1, 'admin', '2021-07-22 11:27:39', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1418050110669901826', '1209733563293962241', 'SQLite', '10', NULL, '', 10, 1, 'admin', '2021-07-22 11:27:47', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1418050149475602434', '1209733563293962241', 'DB2', '11', NULL, '', 11, 1, 'admin', '2021-07-22 11:27:56', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1418050209823248385', '1209733563293962241', 'Hsqldb', '12', NULL, '', 12, 1, 'admin', '2021-07-22 11:28:11', 'admin', '2021-07-22 11:28:27');
INSERT INTO `sys_dict_item` VALUES ('1418050323111399425', '1209733563293962241', 'Derby', '13', NULL, '', 13, 1, 'admin', '2021-07-22 11:28:38', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1418117316707590146', '1209733563293962241', 'H2', '14', NULL, '', 14, 1, 'admin', '2021-07-22 15:54:50', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1418491604048449537', '1209733563293962241', '其他数据库', '15', NULL, '', 15, 1, 'admin', '2021-07-23 16:42:07', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('147c48ff4b51545032a9119d13f3222a', 'd6e1152968b02d69ff358c75b48a6ee1', '测试流程', 'test', NULL, NULL, 1, 1, 'admin', '2019-03-22 19:27:05', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1543fe7e5e26fb97cdafe4981bedc0c8', '4c03fca6bf1f0299c381213961566349', '单排布局', 'single', NULL, NULL, 2, 1, 'admin', '2022-07-12 17:43:39', 'admin', '2019-04-12 17:43:57');
INSERT INTO `sys_dict_item` VALUES ('1600042651777011713', '1600042215909134338', '信息传输、软件和信息技术服务业', '1', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:21:10', 'admin', '2022-12-06 16:21:27');
INSERT INTO `sys_dict_item` VALUES ('1600042736254488578', '1600042215909134338', '制造业', '2', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:21:30', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600042785646612481', '1600042215909134338', '租赁和商务服务业', '3', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:21:42', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600042835433000961', '1600042215909134338', '教育', '4', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:21:54', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600042892072882177', '1600042215909134338', '金融业', '5', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:22:07', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600042975539531778', '1600042215909134338', '建筑业', '6', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:22:27', 'admin', '2022-12-06 16:22:32');
INSERT INTO `sys_dict_item` VALUES ('1600043052177854466', '1600042215909134338', '科学研究和技术服务业', '7', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:22:46', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043101976825857', '1600042215909134338', '批发和零售业', '8', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:22:58', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043157069008898', '1600042215909134338', '住宿和餐饮业', '9', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:23:11', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043203105689601', '1600042215909134338', '电子商务', '10', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:23:22', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043277504253953', '1600042215909134338', '线下零售与服务业', '11', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:23:39', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043334618091521', '1600042215909134338', '文化、体育和娱乐业', '12', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:23:53', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043401030701058', '1600042215909134338', '房地产业', '13', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:24:09', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043476440092673', '1600042215909134338', '交通运输、仓储和邮政业', '14', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:24:27', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043553837584386', '1600042215909134338', '卫生和社会工作', '15', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:24:45', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043628793991170', '1600042215909134338', '公共管理、社会保障和社会组织', '16', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:25:03', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043675329794050', '1600042215909134338', '电力、热力、燃气及水生产和供应业', '18', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:25:14', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043734607892482', '1600042215909134338', '水利、环境和公共设施管理业', '19', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:25:28', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043783068880897', '1600042215909134338', '居民服务、修理和其他服务业', '20', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:25:40', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043822679887874', '1600042215909134338', '政府机构', '21', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:25:49', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043859539431426', '1600042215909134338', '农、林、牧、渔业', '22', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:25:58', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043907551629313', '1600042215909134338', '采矿业', '23', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:26:10', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043955731599362', '1600042215909134338', '国际组织', '24', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:26:21', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600043991685173249', '1600042215909134338', '其他', '25', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:26:30', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600044644096577538', '1600044537800331266', '20人以下', '1', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:29:05', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600044698618335233', '1600044537800331266', '21-99人', '2', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:29:18', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600044744172670978', '1600044537800331266', '100-499人', '3', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:29:29', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600044792306503681', '1600044537800331266', '500-999人', '4', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:29:41', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600044861302804481', '1600044537800331266', '1000-9999人', '5', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:29:57', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1600044924313833473', '1600044537800331266', '10000人以上', '6', NULL, NULL, 1, 1, 'admin', '2022-12-06 16:30:12', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606645562573361153', '1606645341269299201', '总裁/总经理/CEO', '1', NULL, NULL, 1, 1, 'admin', '2022-12-24 21:38:47', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606645619930468354', '1606645341269299201', '副总裁/副总经理/VP', '2', NULL, NULL, 2, 1, 'admin', '2022-12-24 21:39:00', 'admin', '2022-12-24 21:40:00');
INSERT INTO `sys_dict_item` VALUES ('1606645660241924097', '1606645341269299201', '总监/主管/经理', '3', NULL, NULL, 3, 1, 'admin', '2022-12-24 21:39:10', 'admin', '2022-12-24 21:39:41');
INSERT INTO `sys_dict_item` VALUES ('1606645696715591682', '1606645341269299201', '员工/专员/执行', '4', NULL, NULL, 4, 1, 'admin', '2022-12-24 21:39:19', 'admin', '2022-12-24 21:39:37');
INSERT INTO `sys_dict_item` VALUES ('1606645744023146497', '1606645341269299201', '其他', '5', NULL, NULL, 5, 1, 'admin', '2022-12-24 21:39:30', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606647668965412866', '1606646440684457986', '总经办', '1', NULL, NULL, 1, 1, 'admin', '2022-12-24 21:47:09', 'admin', '2023-10-18 13:54:03');
INSERT INTO `sys_dict_item` VALUES ('1606647703098658817', '1606646440684457986', '技术/IT/研发', '2', NULL, NULL, 2, 1, 'admin', '2022-12-24 21:47:17', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606647737919770625', '1606646440684457986', '产品/设计', '3', NULL, NULL, 3, 1, 'admin', '2022-12-24 21:47:25', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606647789614567425', '1606646440684457986', '销售/市场/运营', '4', NULL, '', 4, 1, 'admin', '2022-12-24 21:47:38', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606647827921145857', '1606646440684457986', '人事/财务/行政', '5', NULL, NULL, 5, 1, 'admin', '2022-12-24 21:47:47', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606647860955484162', '1606646440684457986', '资源/仓储/采购', '6', NULL, NULL, 6, 1, 'admin', '2022-12-24 21:47:55', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1606647915473047553', '1606646440684457986', '其他', '7', NULL, NULL, 7, 1, 'admin', '2022-12-24 21:48:08', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1782325511230337025', '83bfb33147013cc81640d5fd9eda030c', '租户操作日志', '3', NULL, NULL, 1, 1, 'admin', '2024-04-22 16:28:11', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1783383857978970114', '83bfb33147013cc81640d5fd9eda030c', '异常日志', '4', NULL, NULL, 3, 1, 'jeecg', '2024-04-25 14:33:40', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1784843259509161986', '1784843187992084482', '电脑终端', 'pc', NULL, NULL, 1, 1, 'jeecg', '2024-04-29 15:12:49', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1784843314429378562', '1784843187992084482', '手机APP端', 'app', NULL, NULL, 1, 1, 'jeecg', '2024-04-29 15:13:02', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1784843380502249474', '1784843187992084482', '移动网页端', 'h5', NULL, NULL, 1, 1, 'jeecg', '2024-04-29 15:13:17', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1890229967585910786', '1890229208685322242', 'OpenAI', 'OPENAI', NULL, NULL, 1, 1, 'jeecg', '2025-02-14 10:41:58', 'jeecg', '2025-02-14 10:42:48');
INSERT INTO `sys_dict_item` VALUES ('1890230018852888577', '1890229208685322242', '智谱AI', 'ZHIPU', NULL, NULL, 1, 1, 'jeecg', '2025-02-14 10:42:10', 'jeecg', '2025-02-14 10:42:42');
INSERT INTO `sys_dict_item` VALUES ('1890230107835047937', '1890229208685322242', '千帆大模型', 'QIANFAN', NULL, NULL, 1, 1, 'jeecg', '2025-02-14 10:42:31', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1890230305948803073', '1890229208685322242', '通义千问', 'QWEN', NULL, NULL, 1, 1, 'jeecg', '2025-02-14 10:43:18', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1890230384159989762', '1890229208685322242', 'DeepSeek', 'DEEPSEEK', NULL, NULL, 1, 1, 'jeecg', '2025-02-14 10:43:37', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1890230437670920194', '1890229208685322242', 'Ollama', 'OLLAMA', NULL, NULL, 1, 1, 'jeecg', '2025-02-14 10:43:50', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1891456733029613569', '1891456510739890177', '语言模型', 'LLM', NULL, NULL, 1, 1, 'jeecg', '2025-02-17 19:56:41', 'jeecg', '2025-02-17 20:02:15');
INSERT INTO `sys_dict_item` VALUES ('1891458099609354241', '1891456510739890177', '向量模型', 'EMBED', NULL, NULL, 1, 1, 'jeecg', '2025-02-17 20:02:07', 'jeecg', '2025-02-17 20:39:01');
INSERT INTO `sys_dict_item` VALUES ('1891672501432479746', '1891672414555860993', '文本', 'text', NULL, NULL, 1, 1, 'jeecg', '2025-02-18 10:14:05', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1891672540963794946', '1891672414555860993', '文件', 'file', NULL, NULL, 1, 1, 'jeecg', '2025-02-18 10:14:14', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1891672567924781058', '1891672414555860993', '网页', 'web', NULL, NULL, 1, 1, 'jeecg', '2025-02-18 10:14:20', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1894701277019959298', '1894701158027554818', '简单配置', 'chatSimple', NULL, NULL, 1, 1, 'jeecg', '2025-02-26 18:49:21', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1894701332930031618', '1894701158027554818', '高级编排', 'chatFLow', NULL, NULL, 2, 1, 'jeecg', '2025-02-26 18:49:34', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1934846897383485441', '1934846825077878786', '发布性通知', '1', NULL, NULL, 1, 1, 'admin', '2025-06-17 13:33:43', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1934846933030875138', '1934846825077878786', '转发性通知', '2', NULL, NULL, 1, 1, 'admin', '2025-06-17 13:33:51', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1934846963749957633', '1934846825077878786', '指示性通知', '3', NULL, NULL, 1, 1, 'admin', '2025-06-17 13:33:59', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1934846993449824257', '1934846825077878786', '任免性通知', '4', NULL, NULL, 1, 1, 'admin', '2025-06-17 13:34:06', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1934847047262744577', '1934846825077878786', '事务性（周知）通知', '5', NULL, NULL, 1, 1, 'admin', '2025-06-17 13:34:18', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1934847082905939969', '1934846825077878786', '会议通知', '6', NULL, NULL, 1, 1, 'admin', '2025-06-17 13:34:27', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1934847117039185921', '1934846825077878786', '其他通知', '7', NULL, NULL, 1, 1, 'admin', '2025-06-17 13:34:35', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1937394006326460418', '1937393911539384322', '通知公告', 'notice', NULL, NULL, 1, 1, 'admin', '2025-06-24 14:15:01', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1937394038412886018', '1937393911539384322', '其他', 'other', NULL, NULL, 1, 1, 'admin', '2025-06-24 14:15:08', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1939572554533429250', '1939572486447292418', '角色', 'ROLE', NULL, NULL, 1, 1, 'admin', '2025-06-30 14:31:47', 'admin', '2025-06-30 15:04:18');
INSERT INTO `sys_dict_item` VALUES ('1939572602289774594', '1939572486447292418', '用户', 'USER', NULL, NULL, 2, 1, 'admin', '2025-06-30 14:31:59', 'admin', '2025-06-30 15:04:21');
INSERT INTO `sys_dict_item` VALUES ('1955230463631126529', '1174511106530525185', '子公司', '4', NULL, NULL, 1, 1, 'admin', '2025-08-12 19:30:44', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1963079150651305985', '1939572486447292418', '全局默认', 'DEFAULT', NULL, NULL, 3, 1, 'admin', '2025-09-03 11:18:36', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1964944982842281986', '1964944899916697602', '董事长', '0', NULL, NULL, 0, 1, 'admin', '2025-09-08 14:52:45', 'admin', '2025-09-08 14:53:54');
INSERT INTO `sys_dict_item` VALUES ('1964945020519714817', '1964944899916697602', '总经理', '1', NULL, NULL, 1, 1, 'admin', '2025-09-08 14:52:54', 'admin', '2025-09-13 18:50:37');
INSERT INTO `sys_dict_item` VALUES ('1964945061850386434', '1964944899916697602', '副总经理', '2', NULL, NULL, 2, 1, 'admin', '2025-09-08 14:53:04', 'admin', '2025-09-08 14:53:46');
INSERT INTO `sys_dict_item` VALUES ('1964945100802887681', '1964944899916697602', '部长', '3', NULL, NULL, 3, 1, 'admin', '2025-09-08 14:53:14', 'admin', '2025-09-08 14:53:43');
INSERT INTO `sys_dict_item` VALUES ('1964945142854979586', '1964944899916697602', '副部长', '4', NULL, NULL, 4, 1, 'admin', '2025-09-08 14:53:24', 'admin', '2025-09-08 14:53:40');
INSERT INTO `sys_dict_item` VALUES ('1964945196395270146', '1964944899916697602', '职员', '5', NULL, NULL, 5, 1, 'admin', '2025-09-08 14:53:36', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1ce390c52453891f93514c1bd2795d44', 'ad7c65ba97c20a6805d5dcdf13cdaf36', '000', '00', NULL, NULL, 1, 1, 'admin', '2019-03-22 16:34:34', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('1db531bcff19649fa82a644c8a939dc4', '4c03fca6bf1f0299c381213961566349', '组合布局', 'combination', NULL, '', 4, 1, 'admin', '2019-05-11 16:07:08', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('222705e11ef0264d4214affff1fb4ff9', '4f69be5f507accea8d5df5f11346181a', '文本', '1', NULL, '', 1, 1, 'admin', '2023-02-28 10:50:36', 'admin', '2022-07-04 16:29:21');
INSERT INTO `sys_dict_item` VALUES ('23a5bb76004ed0e39414e928c4cde155', '4e4602b3e3686f0911384e188dc7efb4', '不等于', '!=', NULL, '不等于', 3, 1, 'admin', '2019-04-01 16:46:15', 'admin', '2019-04-01 17:48:40');
INSERT INTO `sys_dict_item` VALUES ('25847e9cb661a7c711f9998452dc09e6', '4e4602b3e3686f0911384e188dc7efb4', '小于等于', '<=', NULL, '小于等于', 6, 1, 'admin', '2019-04-01 16:44:34', 'admin', '2019-04-01 17:49:10');
INSERT INTO `sys_dict_item` VALUES ('2d51376643f220afdeb6d216a8ac2c01', '68168534ff5065a152bfab275c2136f8', '有效', '1', NULL, '有效', 2, 1, 'admin', '2019-04-26 19:22:01', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('308c8aadf0c37ecdde188b97ca9833f5', '8dfe32e2d29ea9430a988b3b558bf233', '已发布', '1', NULL, '已发布', 2, 1, 'admin', '2019-04-16 17:41:24', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('333e6b2196e01ef9a5f76d74e86a6e33', '8dfe32e2d29ea9430a988b3b558bf233', '未发布', '0', NULL, '未发布', 1, 1, 'admin', '2019-04-16 17:41:12', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('337ea1e401bda7233f6258c284ce4f50', 'bd1b8bc28e65d6feefefb6f3c79f42fd', 'JSON', 'json', NULL, NULL, 1, 1, 'admin', '2019-04-12 17:26:33', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('33bc9d9f753cf7dc40e70461e50fdc54', 'a9d9942bd0eccb6e89de92d130ec4c4a', '发送失败', '2', NULL, NULL, 3, 1, 'admin', '2019-04-12 18:20:02', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('3fbc03d6c994ae06d083751248037c0e', '78bda155fe380b1b3f175f1e88c284c6', '已完成', '3', NULL, '已完成', 3, 1, 'admin', '2019-05-09 16:33:25', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('41d7aaa40c9b61756ffb1f28da5ead8e', '0b5d19e1fce4b2e6647e6b4a17760c14', '通知公告', '1', NULL, NULL, 1, 1, 'admin', '2019-04-22 18:01:57', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('41fa1e9571505d643aea87aeb83d4d76', '4e4602b3e3686f0911384e188dc7efb4', '等于', '=', NULL, '等于', 4, 1, 'admin', '2019-04-01 16:45:24', 'admin', '2019-04-01 17:49:00');
INSERT INTO `sys_dict_item` VALUES ('43d2295b8610adce9510ff196a49c6e9', '845da5006c97754728bf48b6a10f79cc', '正常', '1', NULL, NULL, NULL, 1, 'admin', '2019-03-18 21:45:51', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('4f05fb5376f4c61502c5105f52e4dd2b', '83bfb33147013cc81640d5fd9eda030c', '操作日志', '2', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:22:49', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('50223341bfb5ba30bf6319789d8d17fe', 'd6e1152968b02d69ff358c75b48a6ee1', '业务办理', 'business', NULL, NULL, 3, 1, 'admin', '2023-04-22 19:28:05', 'admin', '2019-03-22 23:24:39');
INSERT INTO `sys_dict_item` VALUES ('51222413e5906cdaf160bb5c86fb827c', 'a7adbcd86c37f7dbc9b66945c82ef9e6', '是', '1', NULL, '', 1, 1, 'admin', '2019-05-22 19:29:45', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('538fca35afe004972c5f3947c039e766', '2e02df51611a4b9632828ab7e5338f00', '显示', '1', NULL, '显示', 1, 1, 'admin', '2025-03-26 18:27:13', 'admin', '2019-04-26 18:39:07');
INSERT INTO `sys_dict_item` VALUES ('5584c21993bde231bbde2b966f2633ac', '4e4602b3e3686f0911384e188dc7efb4', '自定义SQL表达式', 'USE_SQL_RULES', NULL, '自定义SQL表达式', 9, 1, 'admin', '2019-04-01 10:45:24', 'admin', '2019-04-01 17:49:27');
INSERT INTO `sys_dict_item` VALUES ('58b73b344305c99b9d8db0fc056bbc0a', '72cce0989df68887546746d8f09811aa', '主表', '2', NULL, NULL, 2, 1, 'admin', '2019-03-27 10:13:36', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('5b65a88f076b32e8e69d19bbaadb52d5', '2f0320997ade5dd147c90130f7218c3e', '全体用户', 'ALL', NULL, NULL, NULL, 1, 'admin', '2020-10-17 21:22:43', 'admin', '2019-03-28 22:17:09');
INSERT INTO `sys_dict_item` VALUES ('5d833f69296f691843ccdd0c91212b6b', '880a895c98afeca9d9ac39f29e67c13e', '修改', '3', NULL, '', 3, 1, 'admin', '2019-07-22 10:55:07', 'admin', '2019-07-22 10:55:41');
INSERT INTO `sys_dict_item` VALUES ('5d84a8634c8fdfe96275385075b105c9', '3d9a351be3436fbefb1307d4cfb49bf2', '女', '2', NULL, NULL, 2, 1, NULL, '2019-01-04 14:56:56', NULL, '2019-01-04 17:38:12');
INSERT INTO `sys_dict_item` VALUES ('66c952ae2c3701a993e7db58f3baf55e', '4e4602b3e3686f0911384e188dc7efb4', '大于', '>', NULL, '大于', 1, 1, 'admin', '2019-04-01 10:45:46', 'admin', '2019-04-01 17:48:29');
INSERT INTO `sys_dict_item` VALUES ('6937c5dde8f92e9a00d4e2ded9198694', 'ad7c65ba97c20a6805d5dcdf13cdaf36', 'easyui', '3', NULL, NULL, 1, 1, 'admin', '2019-03-22 16:32:15', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('69cacf64e244100289ddd4aa9fa3b915', 'a9d9942bd0eccb6e89de92d130ec4c4a', '未发送', '0', NULL, NULL, 1, 1, 'admin', '2019-04-12 18:19:23', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('6a7a9e1403a7943aba69e54ebeff9762', '4f69be5f507accea8d5df5f11346181a', '富文本', '2', NULL, '', 2, 1, 'admin', '2031-02-28 10:50:44', 'admin', '2022-07-04 16:29:30');
INSERT INTO `sys_dict_item` VALUES ('6c682d78ddf1715baf79a1d52d2aa8c2', '72cce0989df68887546746d8f09811aa', '单表', '1', NULL, NULL, 1, 1, 'admin', '2019-03-27 10:13:29', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('6d404fd2d82311fbc87722cd302a28bc', '4e4602b3e3686f0911384e188dc7efb4', '模糊', 'LIKE', NULL, '模糊', 7, 1, 'admin', '2019-04-01 16:46:02', 'admin', '2019-04-01 17:49:20');
INSERT INTO `sys_dict_item` VALUES ('6d4e26e78e1a09699182e08516c49fc4', '4d7fec1a7799a436d26d02325eff295e', '高', 'H', NULL, '高', 1, 1, 'admin', '2019-04-16 17:04:24', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('700e9f030654f3f90e9ba76ab0713551', '6b78e3f59faec1a4750acff08030a79b', '333', '333', NULL, NULL, NULL, 1, 'admin', '2019-02-21 19:59:47', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('7050c1522702bac3be40e3b7d2e1dfd8', 'c5a14c75172783d72cbee6ee7f5df5d1', '柱状图', 'bar', NULL, NULL, 1, 1, 'admin', '2019-04-12 17:05:17', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('71b924faa93805c5c1579f12e001c809', 'd6e1152968b02d69ff358c75b48a6ee1', 'OA办公', 'oa', NULL, NULL, 2, 1, 'admin', '2021-03-22 19:27:17', 'admin', '2023-10-18 13:54:29');
INSERT INTO `sys_dict_item` VALUES ('75b260d7db45a39fc7f21badeabdb0ed', 'c36169beb12de8a71c8683ee7c28a503', '不启用', '0', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:29:41', 'admin', '2019-03-18 23:29:54');
INSERT INTO `sys_dict_item` VALUES ('7688469db4a3eba61e6e35578dc7c2e5', 'c36169beb12de8a71c8683ee7c28a503', '启用', '1', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:29:28', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('78ea6cadac457967a4b1c4eb7aaa418c', 'fc6cd58fde2e8481db10d3a1e68ce70c', '正常', '1', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:30:28', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('7ccf7b80c70ee002eceb3116854b75cb', 'ac2f7c0c5c5775fcea7e2387bcb22f01', '按钮权限', '2', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:25:40', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('81fb2bb0e838dc68b43f96cc309f8257', 'fc6cd58fde2e8481db10d3a1e68ce70c', '冻结', '2', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:30:37', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('83250269359855501ec4e9c0b7e21596', '4274efc2292239b6f000b153f50823ff', '可见/可访问(授权后可见/可访问)', '1', NULL, '', 1, 1, 'admin', '2019-05-10 17:54:51', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('84778d7e928bc843ad4756db1322301f', '4e4602b3e3686f0911384e188dc7efb4', '大于等于', '>=', NULL, '大于等于', 5, 1, 'admin', '2019-04-01 10:46:02', 'admin', '2019-04-01 17:49:05');
INSERT INTO `sys_dict_item` VALUES ('848d4da35ebd93782029c57b103e5b36', 'c5a14c75172783d72cbee6ee7f5df5d1', '饼图', 'pie', NULL, NULL, 3, 1, 'admin', '2019-04-12 17:05:49', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('84dfc178dd61b95a72900fcdd624c471', '78bda155fe380b1b3f175f1e88c284c6', '处理中', '2', NULL, '处理中', 2, 1, 'admin', '2019-05-09 16:33:01', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('86f19c7e0a73a0bae451021ac05b99dd', 'ac2f7c0c5c5775fcea7e2387bcb22f01', '子菜单', '1', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:25:27', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('8c618902365ca681ebbbe1e28f11a548', '4c753b5293304e7a445fd2741b46529d', '启用', '1', NULL, '', 0, 1, 'admin', '2020-07-18 23:19:27', 'admin', '2019-05-17 14:51:18');
INSERT INTO `sys_dict_item` VALUES ('8cdf08045056671efd10677b8456c999', '4274efc2292239b6f000b153f50823ff', '可编辑(未授权时禁用)', '2', NULL, '', 2, 1, 'admin', '2019-05-10 17:55:38', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('8ff48e657a7c5090d4f2a59b37d1b878', '4d7fec1a7799a436d26d02325eff295e', '中', 'M', NULL, '中', 2, 1, 'admin', '2019-04-16 17:04:40', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('948923658baa330319e59b2213cda97c', '880a895c98afeca9d9ac39f29e67c13e', '添加', '2', NULL, '', 2, 1, 'admin', '2019-07-22 10:54:59', 'admin', '2019-07-22 10:55:36');
INSERT INTO `sys_dict_item` VALUES ('9a96c4a4e4c5c9b4e4d0cbf6eb3243cc', '4c753b5293304e7a445fd2741b46529d', '不启用', '0', NULL, NULL, 1, 1, 'admin', '2019-03-18 23:19:53', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('a1e7d1ca507cff4a480c8caba7c1339e', '880a895c98afeca9d9ac39f29e67c13e', '导出', '6', NULL, '', 6, 1, 'admin', '2019-07-22 12:06:50', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('a2be752dd4ec980afaec1efd1fb589af', '8dfe32e2d29ea9430a988b3b558bf233', '已撤销', '2', NULL, '已撤销', 3, 1, 'admin', '2019-04-16 17:41:39', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('aa0d8a8042a18715a17f0a888d360aa4', 'ac2f7c0c5c5775fcea7e2387bcb22f01', '一级菜单', '0', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:24:52', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('adcf2a1fe93bb99a84833043f475fe0b', '4e4602b3e3686f0911384e188dc7efb4', '包含', 'IN', NULL, '包含', 8, 1, 'admin', '2019-04-01 16:45:47', 'admin', '2019-04-01 17:49:24');
INSERT INTO `sys_dict_item` VALUES ('b029a41a851465332ee4ee69dcf0a4c2', '0b5d19e1fce4b2e6647e6b4a17760c14', '系统消息', '2', NULL, NULL, 1, 1, 'admin', '2019-02-22 18:02:08', 'admin', '2019-04-22 18:02:13');
INSERT INTO `sys_dict_item` VALUES ('b2a8b4bb2c8e66c2c4b1bb086337f393', '3486f32803bb953e7155dab3513dc68b', '正常', '0', NULL, NULL, NULL, 1, 'admin', '2022-10-18 21:46:48', 'admin', '2019-03-28 22:22:20');
INSERT INTO `sys_dict_item` VALUES ('b57f98b88363188daf38d42f25991956', '6b78e3f59faec1a4750acff08030a79b', '22', '222', NULL, NULL, NULL, 0, 'admin', '2019-02-21 19:59:43', 'admin', '2019-03-11 21:23:27');
INSERT INTO `sys_dict_item` VALUES ('b5f3bd5f66bb9a83fecd89228c0d93d1', '68168534ff5065a152bfab275c2136f8', '无效', '0', NULL, '无效', 1, 1, 'admin', '2019-04-26 19:21:49', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('b9fbe2a3602d4a27b45c100ac5328484', '78bda155fe380b1b3f175f1e88c284c6', '待提交', '1', NULL, '待提交', 1, 1, 'admin', '2019-05-09 16:32:35', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('ba27737829c6e0e582e334832703d75e', '236e8a4baff0db8c62c00dd95632834f', '同步', '1', NULL, '同步', 1, 1, 'admin', '2019-05-15 15:28:15', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('bcec04526b04307e24a005d6dcd27fd6', '880a895c98afeca9d9ac39f29e67c13e', '导入', '5', NULL, '', 5, 1, 'admin', '2019-07-22 12:06:41', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('c53da022b9912e0aed691bbec3c78473', '880a895c98afeca9d9ac39f29e67c13e', '查询', '1', NULL, '', 1, 1, 'admin', '2019-07-22 10:54:51', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('c5700a71ad08994d18ad1dacc37a71a9', 'a7adbcd86c37f7dbc9b66945c82ef9e6', '否', '0', NULL, '', 1, 1, 'admin', '2019-05-22 19:29:55', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('cbfcc5b88fc3a90975df23ffc8cbe29c', 'c5a14c75172783d72cbee6ee7f5df5d1', '曲线图', 'line', NULL, NULL, 2, 1, 'admin', '2019-05-12 17:05:30', 'admin', '2019-04-12 17:06:06');
INSERT INTO `sys_dict_item` VALUES ('d217592908ea3e00ff986ce97f24fb98', 'c5a14c75172783d72cbee6ee7f5df5d1', '数据列表', 'table', NULL, NULL, 4, 1, 'admin', '2019-04-12 17:05:56', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('df168368dcef46cade2aadd80100d8aa', '3d9a351be3436fbefb1307d4cfb49bf2', '男', '1', NULL, NULL, 1, 1, NULL, '2027-08-04 14:56:49', 'admin', '2019-03-23 22:44:44');
INSERT INTO `sys_dict_item` VALUES ('e6329e3a66a003819e2eb830b0ca2ea0', '4e4602b3e3686f0911384e188dc7efb4', '小于', '<', NULL, '小于', 2, 1, 'admin', '2019-04-01 16:44:15', 'admin', '2019-04-01 17:48:34');
INSERT INTO `sys_dict_item` VALUES ('e94eb7af89f1dbfa0d823580a7a6e66a', '236e8a4baff0db8c62c00dd95632834f', '不同步', '0', NULL, '不同步', 2, 1, 'admin', '2019-05-15 15:28:28', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('f0162f4cc572c9273f3e26b2b4d8c082', 'ad7c65ba97c20a6805d5dcdf13cdaf36', 'booostrap', '1', NULL, NULL, 1, 1, 'admin', '2021-08-22 16:32:04', 'admin', '2019-03-22 16:33:57');
INSERT INTO `sys_dict_item` VALUES ('f16c5706f3ae05c57a53850c64ce7c45', 'a9d9942bd0eccb6e89de92d130ec4c4a', '发送成功', '1', NULL, NULL, 2, 1, 'admin', '2019-04-12 18:19:43', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('f2a7920421f3335afdf6ad2b342f6b5d', '845da5006c97754728bf48b6a10f79cc', '冻结', '2', NULL, NULL, NULL, 1, 'admin', '2019-03-18 21:46:02', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('f37f90c496ec9841c4c326b065e00bb2', '83bfb33147013cc81640d5fd9eda030c', '登录日志', '1', NULL, NULL, NULL, 1, 'admin', '2019-03-18 23:22:37', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('f753aff60ff3931c0ecb4812d8b5e643', '4c03fca6bf1f0299c381213961566349', '双排布局', 'double', NULL, NULL, 3, 1, 'admin', '2019-04-12 17:43:51', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('f80a8f6838215753b05e1a5ba3346d22', '880a895c98afeca9d9ac39f29e67c13e', '删除', '4', NULL, '', 4, 1, 'admin', '2019-07-22 10:55:14', 'admin', '2019-07-22 10:55:30');
INSERT INTO `sys_dict_item` VALUES ('fcec03570f68a175e1964808dc3f1c91', '4c03fca6bf1f0299c381213961566349', 'Tab风格', 'tab', NULL, NULL, 1, 1, 'admin', '2019-04-12 17:43:31', NULL, NULL);
INSERT INTO `sys_dict_item` VALUES ('fe50b23ae5e68434def76f67cef35d2d', '78bda155fe380b1b3f175f1e88c284c6', '已作废', '4', NULL, '已作废', 4, 1, 'admin', '2021-09-09 16:33:43', 'admin', '2019-05-09 16:34:40');

-- ----------------------------
-- Table structure for sys_files
-- ----------------------------
DROP TABLE IF EXISTS `sys_files`;
CREATE TABLE `sys_files`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `file_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件名称',
  `url` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件地址',
  `file_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文档类型（folder:文件夹 excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）',
  `store_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件上传类型(temp/本地上传(临时文件) manage/知识库)',
  `parent_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父级id',
  `tenant_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '租户id',
  `file_size` double(13, 2) NULL DEFAULT NULL COMMENT '文件大小（kb）',
  `iz_folder` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否文件夹(1：是  0：否)',
  `iz_root_folder` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否为1级文件夹，允许为空 (1：是 )',
  `iz_star` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否标星(1：是  0：否)',
  `down_count` int NULL DEFAULT NULL COMMENT '下载次数',
  `read_count` int NULL DEFAULT NULL COMMENT '阅读次数',
  `share_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '分享链接',
  `share_perms` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '分享权限(1.关闭分享 2.允许所有联系人查看 3.允许任何人查看)',
  `enable_down` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否允许下载(1：是  0：否)',
  `enable_updat` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否允许修改(1：是  0：否)',
  `del_flag` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '删除状态(0-正常,1-删除至回收站)',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `index_del_flag`(`del_flag` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '知识库-文档管理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_files
-- ----------------------------
INSERT INTO `sys_files` VALUES ('1580814573568143361', '11.jpg', 'comment/11_1665730539114.jpg', 'image', 'temp', NULL, NULL, 10956.00, '0', '0', '0', NULL, NULL, NULL, NULL, NULL, NULL, '0', 'admin', '2022-10-14 14:55:39', NULL, NULL);
INSERT INTO `sys_files` VALUES ('1584493984691740674', 'jeecg-boot漏洞.pdf', 'comment/jeecg-boot漏洞_1666607779077.pdf', 'pdf', 'temp', NULL, NULL, 842789.00, '0', '0', '0', NULL, NULL, NULL, NULL, NULL, NULL, '0', 'admin', '2022-10-24 18:36:19', NULL, NULL);
INSERT INTO `sys_files` VALUES ('1714455459928985601', '低代码平台.png', 'comment/低代码平台_1697593009343.png', 'image', 'temp', NULL, '1000', 1249631.00, '0', '0', '0', NULL, NULL, NULL, NULL, NULL, NULL, '0', 'admin', '2023-10-18 09:36:49', NULL, NULL);

-- ----------------------------
-- Table structure for sys_fill_rule
-- ----------------------------
DROP TABLE IF EXISTS `sys_fill_rule`;
CREATE TABLE `sys_fill_rule`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键ID',
  `rule_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则名称',
  `rule_code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则Code',
  `rule_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则实现类',
  `rule_params` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '规则参数',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sfr_rule_code`(`rule_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_fill_rule
-- ----------------------------
INSERT INTO `sys_fill_rule` VALUES ('1202551334738382850', '机构编码生成', 'org_num_role', 'org.jeecg.modules.system.rule.OrgCodeRule', '{\"parentId\":\"c6d7cb4deeac411cb3384b1b31278596\"}', 'admin', '2019-12-09 10:37:06', 'admin', '2019-12-05 19:32:35');
INSERT INTO `sys_fill_rule` VALUES ('1202787623203065858', '分类字典编码生成', 'category_code_rule', 'org.jeecg.modules.system.rule.CategoryCodeRule', '{\"pid\":\"\"}', 'admin', '2022-10-13 16:47:52', 'admin', '2019-12-06 11:11:31');
INSERT INTO `sys_fill_rule` VALUES ('1260134137920090113', '订单流水号', 'shop_order_num', 'org.jeecg.modules.online.cgform.rule.OrderNumberRule', '{}', 'admin', '2020-12-07 18:29:50', 'admin', '2020-05-12 17:06:05');

-- ----------------------------
-- Table structure for sys_form_file
-- ----------------------------
DROP TABLE IF EXISTS `sys_form_file`;
CREATE TABLE `sys_form_file`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `table_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '表名',
  `table_data_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据id',
  `file_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '关联文件id',
  `file_type` varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '文件类型(text:文本, excel:excel doc:word ppt:ppt image:图片  archive:其他文档 video:视频 pdf:pdf）)',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_table_form`(`table_name` ASC, `table_data_id` ASC) USING BTREE,
  INDEX `index_file_id`(`file_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_form_file
-- ----------------------------
INSERT INTO `sys_form_file` VALUES ('1580814573635252225', 'sys_comment', '1580814573433925634', '1580814573568143361', 'image', 'admin', '2022-10-14 14:55:39');
INSERT INTO `sys_form_file` VALUES ('1584493984716906497', 'sys_comment', '1584493984364584961', '1584493984691740674', 'pdf', 'admin', '2022-10-24 18:36:19');
INSERT INTO `sys_form_file` VALUES ('1714455459928985602', 'sys_comment', '1714455459606024193', '1714455459928985601', 'image', 'admin', '2023-10-18 09:36:49');
INSERT INTO `sys_form_file` VALUES ('1800557956199825409', 'sys_comment', '1800557955415490562', '1800557956132716545', 'image', 'admin', '2024-06-11 23:57:24');
INSERT INTO `sys_form_file` VALUES ('1800558014597120003', 'sys_comment', '1800558013942808578', '1800558014597120002', 'image', 'admin', '2024-06-11 23:57:38');
INSERT INTO `sys_form_file` VALUES ('1805421721449791490', 'sys_comment', '1805421721126830082', '1805421721449791489', 'image', 'admin', '2024-06-25 10:04:16');

-- ----------------------------
-- Table structure for sys_gateway_route
-- ----------------------------
DROP TABLE IF EXISTS `sys_gateway_route`;
CREATE TABLE `sys_gateway_route`  (
  `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `router_id` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '路由ID',
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务名',
  `uri` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '服务地址',
  `predicates` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '断言',
  `filters` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '过滤器',
  `retryable` int NULL DEFAULT NULL COMMENT '是否重试:0-否 1-是',
  `strip_prefix` int NULL DEFAULT NULL COMMENT '是否忽略前缀0-否 1-是',
  `persistable` int NULL DEFAULT NULL COMMENT '是否为保留数据:0-否 1-是',
  `show_api` int NULL DEFAULT NULL COMMENT '是否在接口文档中展示:0-否 1-是',
  `status` int NULL DEFAULT NULL COMMENT '状态:0-无效 1-有效',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属部门',
  `del_flag` int NULL DEFAULT NULL COMMENT '删除状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_gateway_route
-- ----------------------------
INSERT INTO `sys_gateway_route` VALUES ('1331051599401857026', 'jeecg-demo-websocket', 'jeecg-demo-websocket', 'lb:ws://jeecg-demo', '[{\"args\":[\"/vxeSocket/**\"],\"name\":\"Path\"}]', '[{\"args\":[{\"value\":\"#{@ipKeyResolver}\",\"key\":\"key-resolver\"},{\"value\":20,\"key\":\"redis-rate-limiter.replenishRate\"},{\"value\":20,\"key\":\"redis-rate-limiter.burstCapacity\"}],\"name\":\"RequestRateLimiter\",\"title\":\"限流过滤器\"}]', NULL, NULL, NULL, NULL, 1, 'admin', '2020-11-24 09:46:46', NULL, NULL, NULL, 0);
INSERT INTO `sys_gateway_route` VALUES ('1805444036892016641', 'jeecg-erp', 'jeecg-erp', 'lb://jeecg-erp', '[{\"args\":[\"/erp/**\"],\"name\":\"Path\"}]', '[]', NULL, NULL, NULL, NULL, 1, 'admin', '2024-06-25 11:32:57', NULL, NULL, NULL, 0);
INSERT INTO `sys_gateway_route` VALUES ('jeecg-cloud-websocket', 'jeecg-system-websocket', 'jeecg-system-websocket', 'lb:ws://jeecg-system', '[{\"args\":[\"/websocket/**\",\"/eoaSocket/**\",\"/newsWebsocket/**\",\"/dragChannelSocket/**\"],\"name\":\"Path\"}]', '[]', NULL, NULL, NULL, NULL, 1, 'admin', '2020-11-16 19:41:51', NULL, NULL, NULL, 0);
INSERT INTO `sys_gateway_route` VALUES ('jeecg-demo', 'jeecg-demo', 'jeecg-demo', 'lb://jeecg-demo', '[{\"args\":[\"/mock/**\",\"/bigscreen/template1/**\",\"/bigscreen/template2/**\",\"/test/**\",\"/hello/**\"],\"name\":\"Path\"}]', '[]', NULL, NULL, NULL, NULL, 1, 'admin', '2020-11-16 19:41:51', NULL, NULL, NULL, 0);
INSERT INTO `sys_gateway_route` VALUES ('jeecg-system', 'jeecg-system', 'jeecg-system', 'lb://jeecg-system', '[{\"args\":[\"/sys/**\",\"/online/**\",\"/bigscreen/**\",\"/jmreport/**\",\"/druid/**\",\"/generic/**\",\"/actuator/**\",\"/drag/**\",\"/oauth2/**\",\"/defa/**\",\"/demo/**\",\"/jimubi/**\",\"/airag/**\",\"/openapi/**\"],\"name\":\"Path\"}]', '[]', NULL, NULL, NULL, NULL, 1, 'admin', '2020-11-16 19:41:51', NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for sys_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `log_type` int NULL DEFAULT NULL COMMENT '日志类型（1登录日志，2操作日志, 3.租户操作日志）',
  `log_content` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '日志内容',
  `operate_type` int NULL DEFAULT NULL COMMENT '操作类型',
  `userid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '操作用户账号',
  `username` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '操作用户名称',
  `ip` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'IP',
  `method` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求java方法',
  `request_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求路径',
  `request_param` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '请求参数',
  `request_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求类型',
  `cost_time` bigint NULL DEFAULT NULL COMMENT '耗时',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` int NULL DEFAULT NULL COMMENT '租户ID',
  `client_type` varchar(5) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '客户端类型 pc:电脑端 app:手机端 h5:移动网页端',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sl_userid`(`userid`) USING BTREE,
  INDEX `idx_sl_log_type`(`log_type`) USING BTREE,
  INDEX `idx_sl_operate_type`(`operate_type`) USING BTREE,
  INDEX `idx_sl_create_time`(`create_time`) USING BTREE
) ENGINE = MyISAM AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '系统日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `parent_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '父id',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '菜单标题',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '路径',
  `component` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '组件',
  `is_route` tinyint(1) NULL DEFAULT 1 COMMENT '是否路由菜单: 0:不是  1:是（默认值1）',
  `component_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '组件名字',
  `redirect` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '一级菜单跳转地址',
  `menu_type` int NULL DEFAULT NULL COMMENT '菜单类型(0:一级菜单; 1:子菜单:2:按钮权限)',
  `perms` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '菜单权限编码',
  `perms_type` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '权限策略1显示2禁用',
  `sort_no` double(8, 2) NULL DEFAULT NULL COMMENT '菜单排序',
  `always_show` tinyint(1) NULL DEFAULT NULL COMMENT '聚合子路由: 1是0否',
  `icon` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `is_leaf` tinyint(1) NULL DEFAULT NULL COMMENT '是否叶子节点:    1是0否',
  `keep_alive` tinyint(1) NULL DEFAULT NULL COMMENT '是否缓存该页面:    1:是   0:不是',
  `hidden` tinyint NULL DEFAULT 0 COMMENT '是否隐藏路由: 0否,1是',
  `hide_tab` tinyint NULL DEFAULT NULL COMMENT '是否隐藏tab: 0否,1是',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_by` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `del_flag` int NULL DEFAULT 0 COMMENT '删除状态 0正常 1已删除',
  `rule_flag` int NULL DEFAULT 0 COMMENT '是否添加数据权限1是0否',
  `status` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '按钮权限状态(0无效1有效)',
  `internal_or_external` tinyint(1) NULL DEFAULT NULL COMMENT '外链菜单打开方式 0/内部打开 1/外部打开',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `index_menu_type`(`menu_type` ASC) USING BTREE,
  INDEX `index_menu_hidden`(`hidden` ASC) USING BTREE,
  INDEX `index_menu_status`(`status` ASC) USING BTREE,
  INDEX `index_menu_del_flag`(`del_flag` ASC) USING BTREE,
  INDEX `index_menu_url`(`url` ASC) USING BTREE,
  INDEX `index_menu_sort_no`(`sort_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '菜单权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES ('1170592628746878978', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '菜单管理', '/system/menu', 'system/menu/index', 1, NULL, NULL, 1, NULL, '1', 0.00, 0, 'ant-design:menu-fold-outlined', 0, 0, 0, 0, NULL, 'admin', '2019-09-08 15:00:05', 'ceshi', '2023-10-18 12:02:41', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('119213522910765570', '1674708136602542082', '租户用户', '/system/tenant/TenantUserList', 'system/tenant/TenantUserList', 1, 'tenant-system-user', NULL, 1, NULL, NULL, 2.00, 0, 'ant-design:user', 1, 0, 0, 0, NULL, NULL, '2018-12-25 20:34:38', 'admin', '2025-08-12 18:23:19', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1211885237487923202', '1207203817658105858', 'btn:add', '', '', 1, NULL, NULL, 2, 'btn:add', '1', 1.00, 0, NULL, 1, 0, 0, NULL, NULL, 'admin', '2019-12-31 13:42:11', 'admin', '2020-01-07 20:07:53', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1214376304951664642', '3f915b2769fc80648e92d04e84ca059d', '用户编辑', '', '', 0, NULL, NULL, 2, 'system:user:edit', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2020-01-07 10:40:47', 'admin', '2022-11-17 16:24:33', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1214462306546319322', '119213522910765570', '新增用户', '', '', 1, NULL, NULL, 2, 'system:user:addTenantUser', '1', 1.00, 0, NULL, 1, 0, 0, NULL, NULL, 'admin', '2020-01-07 16:22:32', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1214462306546319362', '3f915b2769fc80648e92d04e84ca059d', '新增用户', '', '', 0, NULL, NULL, 2, 'system:user:add', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2020-01-07 16:22:32', 'admin', '2022-11-17 16:24:47', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1280350452934307841', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '多租户管理', '/system/tenant', 'system/tenant/index', 1, NULL, NULL, 1, NULL, '1', 6.00, 0, 'ant-design:appstore-twotone', 0, 0, 0, 0, NULL, 'admin', '2020-07-07 11:58:30', 'admin', '2025-06-25 14:16:36', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1438108176273760258', '', '主页', '/dashboard', 'layouts/default/index', 1, NULL, '/dashboard/analysis', 0, NULL, '1', 1.00, 0, 'ant-design:home-outlined', 0, 0, 0, 0, NULL, 'admin', '2021-09-15 19:51:23', 'admin', '2025-09-13 18:49:01', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1438108176814825473', '1438108176273760258', '工作台', '/dashboard/workbench', 'dashboard/workbench/index', 1, NULL, NULL, 1, NULL, '1', 2.00, 0, 'ant-design:appstore-twotone', 1, 0, 1, 0, NULL, 'admin', '2021-09-15 19:51:23', 'jeecg', '2024-06-13 11:37:46', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1438469604861403137', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '职务级别', '/system/position', 'system/position/index', 1, NULL, NULL, 1, NULL, '0', 5.00, 0, 'ant-design:database-filled', 1, 0, 0, NULL, NULL, 'admin', '2021-09-16 19:47:33', 'admin', '2021-09-17 15:58:22', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1438782530717495298', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '分类字典', '/system/category', 'system/category/index', 1, NULL, NULL, 1, NULL, '0', 9.00, 0, 'ant-design:group-outlined', 1, 0, 0, NULL, NULL, 'admin', '2021-09-17 16:31:01', NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1438782641187074050', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '数据字典', '/system/dict', 'system/dict/index', 1, NULL, NULL, 1, NULL, '0', 4.00, 0, 'ant-design:hdd-twotone', 0, 0, 0, 0, NULL, 'admin', '2021-09-17 16:31:27', 'admin', '2023-03-04 15:01:55', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1438782851980210178', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '通知公告', '/system/notice', 'system/notice/index', 1, NULL, NULL, 1, NULL, '0', 8.00, 0, 'ant-design:bell-outlined', 1, 0, 0, NULL, NULL, 'admin', '2021-09-17 16:32:17', 'admin', '2021-09-17 16:36:15', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1455735714507472898', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '通讯录', '/address', 'system/address/index', 1, NULL, NULL, 1, NULL, '0', 20.00, 0, 'ant-design:book-outlined', 1, 0, 0, 0, NULL, 'admin', '2021-11-03 11:16:55', 'admin', '2023-10-18 13:54:58', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1588513553652436993', '3f915b2769fc80648e92d04e84ca059d', '修改密码', NULL, NULL, 0, NULL, NULL, 2, 'system:user:changepwd', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-04 20:48:39', 'admin', '2022-11-04 20:49:06', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592102143467200514', '1597419994965786625', '给指定角色添加用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:addUserRole', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:18:49', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592112984361365505', '1170592628746878978', '添加菜单', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:11:30', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592113148350263298', '190c2b43bec6a5f7a4194a85db67d96a', '保存角色授权', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:saveRole', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:12:09', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114574275211265', '3f915b2769fc80648e92d04e84ca059d', '删除用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:17:49', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114574275211345', '119213522910765570', '删除用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:17:49', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114652566089729', '3f915b2769fc80648e92d04e84ca059d', '批量删除用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:18:08', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114721138765826', '3f915b2769fc80648e92d04e84ca059d', '冻结/解冻用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:frozenBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:18:24', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114772665790465', '3f915b2769fc80648e92d04e84ca059d', '首页用户重置密码', NULL, NULL, 0, NULL, NULL, 2, 'system:user:updatepwd', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:18:37', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114823467200514', '3f915b2769fc80648e92d04e84ca059d', '给指定角色添加用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:addUserRole', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:18:49', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114893302362114', '3f915b2769fc80648e92d04e84ca059d', '删除指定角色的用户关系', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteRole', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:19:05', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114893302823614', '1597419994965786625', '删除指定角色的用户关系', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteRole', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:19:05', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114955650691074', '3f915b2769fc80648e92d04e84ca059d', '批量删除指定角色的用户关系', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteRoleBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:19:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592114955650691174', '1597419994965786625', '批量删除指定角色的用户关系', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteRoleBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:19:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115070432014338', '3f915b2769fc80648e92d04e84ca059d', '给指定部门添加对应的用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:editDepartWithUser', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:19:48', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115115361398786', '3f915b2769fc80648e92d04e84ca059d', '删除指定机构的用户关系', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteUserInDepart', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:19:58', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115162379546625', '3f915b2769fc80648e92d04e84ca059d', '批量删除指定机构的用户关系', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteUserInDepartBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:20:09', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115213910765570', '3f915b2769fc80648e92d04e84ca059d', '彻底删除用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:deleteRecycleBin', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:20:22', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115610431877122', '1439511654494937090', '多数据源分页列表', NULL, NULL, 0, NULL, NULL, 2, 'system:datasource:list', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:21:56', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115712422330529', '1961009998209257473', '部门添加', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:22:21', 'admin', '2022-11-14 19:30:49', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115712466710529', '45c966826eeff4c99b8f8ebfe74511fc', '部门添加', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:22:21', 'admin', '2022-11-14 19:30:49', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592115914493751297', '1170592628746878978', '编辑菜单权限数据', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:editRule', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:23:09', 'admin', '2022-11-14 19:39:25', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592116663936184322', '1170592628746878978', '编辑菜单', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:26:07', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117222764277032', '1961009998209257473', '部门编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:28:21', 'admin', '2022-11-14 19:30:55', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117222764277761', '45c966826eeff4c99b8f8ebfe74511fc', '部门编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:28:21', 'admin', '2022-11-14 19:30:55', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117276539449345', '45c966826eeff4c99b8f8ebfe74511fc', '部门删除', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:28:33', 'admin', '2022-11-14 19:31:06', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117276539449346', '1961009998209257473', '部门删除', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:28:33', 'admin', '2022-11-14 19:31:06', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117377299214337', '45c966826eeff4c99b8f8ebfe74511fc', '部门批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:28:58', 'admin', '2022-11-14 19:31:12', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117377299214338', '1961009998209257473', '部门批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:28:58', 'admin', '2022-11-14 19:31:12', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117422006300673', '45c966826eeff4c99b8f8ebfe74511fc', '部门导入', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:29:08', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117625664925697', '5c2f42277948043026b7a14692456828', '部门角色添加', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:role:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:29:57', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117748209905665', '5c2f42277948043026b7a14692456828', '部门角色编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:role:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:30:26', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117804359053314', '5c2f42277948043026b7a14692456828', '部门角色删除', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:role:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:30:39', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592117990305132545', '5c2f42277948043026b7a14692456828', '部门角色批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:role:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:31:24', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118053634928641', '5c2f42277948043026b7a14692456828', '部门角色用户授权', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:role:userAdd', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:31:39', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118192218927105', '1438782641187074050', '字典新增', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:32:12', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118254844080130', '1438782641187074050', '字典编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:32:27', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118306983473154', '1438782641187074050', '字典删除', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:32:39', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118356778250241', '1438782641187074050', '字典批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:32:51', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118414990995457', '1438782641187074050', '字典导入', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:33:05', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118497606201346', '1439399179791409153', '路由网关删除', NULL, NULL, 0, NULL, NULL, 2, 'system:getway:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:33:25', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118604640645122', '1170592628746878978', '删除菜单', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:33:50', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592118648315932674', '1170592628746878978', '批量删除菜单', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:34:01', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592119001883176961', '1170592628746878978', '添加菜单权限数据', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:addRule', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:35:25', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120052866707457', '1170592628746878978', '删除菜单权限数据', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:deleteRule', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:39:35', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120222727630849', '45c966826eeff4c99b8f8ebfe74511fc', '保存部门授权', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:saveDepart', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:40:16', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120224120850434', '190c2b43bec6a5f7a4194a85db67d96a', '查询全部角色不租户隔离', NULL, NULL, 0, NULL, NULL, 2, 'system:role:queryallNoByTenant', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-11 19:41:18', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120323667750914', '190c2b43bec6a5f7a4194a85db67d96a', '角色添加', NULL, NULL, 0, NULL, NULL, 2, 'system:role:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:40:40', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120323667750934', '1597419994965786625', '角色添加', NULL, NULL, 0, NULL, NULL, 2, 'system:role:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:40:40', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120372296511490', '190c2b43bec6a5f7a4194a85db67d96a', '角色编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:role:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:40:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120372296522490', '1597419994965786625', '角色编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:role:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:40:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120427007012865', '190c2b43bec6a5f7a4194a85db67d96a', '角色删除', NULL, NULL, 0, NULL, NULL, 2, 'system:role:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:41:05', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120427223412865', '1597419994965786625', '角色删除', NULL, NULL, 0, NULL, NULL, 2, 'system:role:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:41:05', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120484120850434', '190c2b43bec6a5f7a4194a85db67d96a', '角色批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:role:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:41:18', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120594695286785', '190c2b43bec6a5f7a4194a85db67d96a', '角色首页配置添加', NULL, NULL, 0, NULL, NULL, 2, 'system:roleindex:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:41:45', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592120649007329281', '190c2b43bec6a5f7a4194a85db67d96a', '角色首页配置编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:roleindex:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:41:58', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1592135223910765570', '3f915b2769fc80648e92d04e84ca059d', '查询全部用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:listAll', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:20:22', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593160905216663554', '1438782641187074050', '字典子项新增', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:item:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:35:34', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593160959633563650', '1438782641187074050', '字典子项编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:item:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:35:47', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161025790320641', '1438782641187074050', '字典子项删除', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:item:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:36:03', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161089787011074', '1438782641187074050', '字典子项批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:item:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:36:18', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161421350936578', '1439488251473993730', '添加定时任务', NULL, NULL, 0, NULL, NULL, 2, 'system:quartzJob:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:37:37', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161483627962370', '1439488251473993730', '更新定时任务', NULL, NULL, 0, NULL, NULL, 2, 'system:quartzJob:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:37:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161551202394114', '1439488251473993730', '删除定时任务', NULL, NULL, 0, NULL, NULL, 2, 'system:quartzJob:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:38:08', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161608362369026', '1439488251473993730', '批量删除定时任务', NULL, NULL, 0, NULL, NULL, 2, 'system:quartzJob:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:38:22', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161657385394177', '1439488251473993730', '停止定时任务', NULL, NULL, 0, NULL, NULL, 2, 'system:quartzJob:pause', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:38:33', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161697348722689', '1439488251473993730', '启动定时任务', NULL, NULL, 0, NULL, NULL, 2, 'system:quartzJob:resume', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:38:43', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593161743607701505', '1439488251473993730', '立即执行定时任务', NULL, NULL, 0, NULL, NULL, 2, 'system:quartzJob:execute', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 16:38:54', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1593185714482880514', '3f915b2769fc80648e92d04e84ca059d', '用户导出', NULL, NULL, 0, NULL, NULL, 2, 'system:user:export', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-17 18:14:09', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1594930803956920321', '1439398677984878593', '在线用户', '/system/onlineuser', 'system/onlineuser/OnlineUserList', 1, '', NULL, 1, NULL, '0', 12.00, 0, 'ant-design:aliwangwang-outlined', 1, 0, 0, 0, NULL, 'admin', '2022-11-22 13:48:31', 'admin', '2023-03-04 15:15:36', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1596141938193747970', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '用户设置', '/system/usersetting', 'system/usersetting/UserSetting', 1, '', NULL, 1, NULL, '0', 12.00, 0, 'ant-design:setting-twotone', 0, 0, 1, 0, NULL, 'admin', '2022-11-25 22:01:08', 'admin', '2023-03-04 15:00:26', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1596335805278990338', '1596141938193747970', '账户设置用户编辑权限', NULL, NULL, 0, NULL, NULL, 2, 'system:user:setting:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-26 10:51:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1597419994965786625', '1674708136602542082', '租户角色', '/system/role/TenantRoleList', 'system/role/TenantRoleList', 1, 'tenant-role-list', NULL, 1, NULL, '0', 3.00, 0, 'ant-design:line-height-outlined', 0, 0, 0, 0, NULL, 'admin', '2022-11-29 10:39:40', 'admin', '2025-08-12 18:23:22', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('15c92115213910765570', '3f915b2769fc80648e92d04e84ca059d', '通过ID查询用户信息接口', NULL, NULL, 0, NULL, NULL, 2, 'system:user:queryById', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:20:22', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1600105607009162230', '1961253156897710081', '邀请用户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:invitation:user', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-06 20:31:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1600105607009165314', '1280350452934307841', '邀请用户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:invitation:user', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-06 20:31:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1600108123037913486', '1961253156897710081', '查询租户下用户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:user:list', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-06 20:41:20', 'admin', '2023-01-11 12:10:48', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1600108123037917186', '1280350452934307841', '通过租户id获取用户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:user:list', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-06 20:41:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1600129606082650113', '1280350452934307841', '租户请离', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:leave', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-06 22:06:42', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1600129606082650123', '119213522910765570', '租户请离', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:leave', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-06 22:06:42', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1609123240547344376', '1961253156897710081', '产品包分页列表查询', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:packList', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-31 17:44:11', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1609123240547344385', '1280350452934307841', '产品包分页列表查询', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:packList', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-31 17:44:11', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1609123437247619074', '1280350452934307841', '创建租户产品包', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:add:pack', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-31 17:44:58', 'admin', '2022-12-31 20:27:56', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1609164542165012482', '1280350452934307841', '编辑租户产品包', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:edit:pack', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-31 20:28:18', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1609164635442139138', '1280350452934307841', '批量删除租户产品包', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:delete:pack', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-12-31 20:28:41', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1611620416187969538', '1280350452934307841', '分页获取租户用户数据', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:tenantPageList', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-07 15:07:04', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1611620600003342337', '1280350452934307841', '通过用户id获取租户列表', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:getTenantListByUserId', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-07 15:07:48', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1611620654621569026', '1280350452934307841', '更新用户租户关系状态', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:updateUserTenantStatus', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-07 15:08:01', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1611620772498218641', '1280350452934307841', '查询租户列表', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:list', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-11 15:08:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1611620772498288641', '1280350452934307841', '注销租户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:cancelTenant', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-07 15:08:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1611650772498288641', '1280350452934307841', '删除租户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-11 15:08:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1612438989792034818', '1280350452934307841', '编辑租户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-07 15:08:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1613620712498288641', '1280350452934307841', '批量删除租户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-11 15:08:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1620261087828418562', '1280350452934307841', '获取租户删除的列表', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:recycleBinPageList', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-31 11:22:01', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1620305415648989186', '1280350452934307841', '彻底删除租户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:deleteTenantLogic', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-31 14:18:10', 'admin', '2023-01-31 14:19:51', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1620327825894981634', '1280350452934307841', '租户还原', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:revertTenantLogic', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-31 15:47:13', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1621620772498288641', '1280350452934307841', '添加租户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-11 15:08:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1629109281748291586', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '第三方配置', '/third/app', 'system/appconfig/ThirdAppConfigList', 1, '', NULL, 1, NULL, '0', 13.00, 0, 'ant-design:setting-outlined', 1, 0, 0, 0, NULL, 'admin', '2023-02-24 21:21:35', 'admin', '2023-02-24 21:51:05', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1660568280725127169', '1439533711676973057', '日志列表', NULL, NULL, 1, NULL, NULL, 2, 'system:log:list', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-05-22 16:48:25', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1660568368558047234', '1439533711676973057', '日志删除', NULL, NULL, 1, NULL, NULL, 2, 'system:log:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-05-22 16:48:46', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1660568426632380417', '1439533711676973057', '日志批量删除', NULL, NULL, 1, NULL, NULL, 2, 'system:log:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-05-22 16:48:59', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1668174661456171010', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '租户默认套餐', '/tenant/TenantDefaultPack', 'system/tenant/pack/TenantDefaultPackList', 1, '', NULL, 1, NULL, '0', 7.00, 0, 'ant-design:deployment-unit-outlined', 1, 0, 0, 0, NULL, 'admin', '2023-06-12 16:33:27', 'admin', '2025-07-30 17:34:03', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1674708136602542082', '', '我的租户', '/mytenant', 'layouts/RouteView', 1, '', NULL, 0, NULL, '0', 4.20, 0, 'ant-design:user-outlined', 0, 0, 0, 0, NULL, 'admin', '2023-06-30 17:15:09', 'admin', '2024-06-17 15:42:29', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1693195557097164801', '190c2b43bec6a5f7a4194a85db67d96a', '查询所有角色', NULL, NULL, 0, NULL, NULL, 2, 'system:role:list', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-08-20 17:37:34', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1697220712498288641', '1280350452934307841', '根据ids查询租户', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:queryList', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2023-01-11 15:08:29', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1701575168519839746', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '白名单管理', '/system/tableWhiteList', 'system/tableWhiteList/SysTableWhiteListList', 1, '', NULL, 1, NULL, '0', 4.00, 0, 'ant-design:table-outlined', 0, 0, 0, 0, NULL, 'admin', '2023-09-12 20:35:09', 'jeecg', '2024-06-13 11:38:28', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1750128461040648193', '1170592628746878978', '设置默认首页', NULL, NULL, 0, NULL, NULL, 2, 'system:permission:setDefIndex', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2024-01-24 20:08:35', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1800372628805861377', '1701575168519839746', '列表权限', NULL, NULL, 0, NULL, NULL, 2, 'system:tableWhite:list', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-06-11 11:40:59', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1800372727493640194', '1701575168519839746', '添加权限', NULL, NULL, 0, NULL, NULL, 2, 'system:tableWhite:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-06-11 11:41:22', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1800372811518132225', '1701575168519839746', '修改权限', NULL, NULL, 0, NULL, NULL, 2, 'system:tableWhite:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-06-11 11:41:42', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1800372906330374146', '1701575168519839746', '删除权限', NULL, NULL, 0, NULL, NULL, 2, 'system:tableWhite:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-06-11 11:42:05', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1800373633509441537', '1701575168519839746', '批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:tableWhite:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-06-11 11:44:58', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1800373733220630530', '1701575168519839746', '通过id查询', NULL, NULL, 0, NULL, NULL, 2, 'system:tableWhite:queryById', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-06-11 11:45:22', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1808098125316870145', '3f915b2769fc80648e92d04e84ca059d', 'app端编辑用户', NULL, NULL, 0, NULL, NULL, 2, 'system:user:app:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2024-07-02 19:19:21', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1810652607946940417', '1438782641187074050', '批量彻底删除', NULL, NULL, 0, NULL, NULL, 2, 'system:dict:deleteRecycleBin', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-07-09 20:29:57', '15931993294', '2024-07-09 20:30:39', 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1810923799513612290', '1439399179791409153', '彻底删除', NULL, NULL, 0, NULL, NULL, 2, 'system:gateway:deleteRecycleBin', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-07-10 14:27:34', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1811685368354754561', '1439399179791409153', '复制路由', NULL, NULL, 0, NULL, NULL, 2, 'system:gateway:copyRoute', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-07-12 16:53:46', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1811685464467230721', '1439399179791409153', '还原逻辑删除', NULL, NULL, 0, NULL, NULL, 2, 'system:gateway:putRecycleBin', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, '15931993294', '2024-07-12 16:54:09', NULL, NULL, 0, NULL, '1', 0);
INSERT INTO `sys_permission` VALUES ('1887447660072292354', '1280350452934307841', '初始化套餐包', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:syncDefaultPack', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'jeecg', '2025-02-06 18:26:04', 'jeecg', '2025-02-06 18:26:53', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1890213291321749505', '1892553163993931777', 'AI流程设计', '/process/list/airag', 'super/airag/aiflow/pages/ProcessList', 1, '', NULL, 1, NULL, '0', 3.00, 0, 'ant-design:box-plot-outlined', 0, 0, 0, 0, NULL, 'admin', '2025-02-14 09:35:41', 'admin', '2025-03-06 20:31:08', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1892553163993931777', '', 'AI应用平台', '/airag', 'layouts/default/index', 1, '', NULL, 0, NULL, '0', 2.20, 0, 'ant-design:box-plot-outlined', 0, 0, 0, 0, NULL, 'admin', '2025-02-20 20:33:31', 'admin', '2025-11-25 16:20:04', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1892553778493022209', '1892553163993931777', 'AI模型配置', '/super/airag/aimodel/AiModelList', 'super/airag/aimodel/AiModelList', 1, '', NULL, 1, NULL, '0', 4.00, 0, 'ant-design:setting-twotone', 0, 0, 0, 0, NULL, 'admin', '2025-02-20 20:35:57', 'admin', '2025-03-06 20:31:13', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1892557342028226561', '1892553163993931777', 'AI知识库', '/super/airag/aiknowledge/AiKnowledgeBaseList', 'super/airag/aiknowledge/AiKnowledgeBaseList', 1, '', NULL, 1, NULL, '0', 2.00, 0, 'ant-design:book-twotone', 0, 0, 0, 0, NULL, 'admin', '2025-02-20 20:50:07', 'admin', '2025-02-23 17:39:01', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1893865471550578689', '1892553163993931777', 'AI应用管理', '/super/airag/aiapp/AiAppList', 'super/airag/aiapp/AiAppList', 1, '', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:appstore-twotone', 0, 0, 0, 0, NULL, 'admin', '2025-02-24 11:28:09', 'admin', '2025-03-06 20:30:58', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1895401981290643458', '1892553163993931777', 'AI聊天', '/super/airag/aiapp/chat/AiChat', 'super/airag/aiapp/chat/AiChat', 1, '', NULL, 1, NULL, '0', 5.00, 0, 'ant-design:aliwangwang-outlined', 1, 0, 1, 0, NULL, 'admin', '2025-02-28 17:13:42', 'admin', '2025-02-28 17:30:40', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('190c2b43bec6a5f7a4194a85db67d96a', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '角色管理', '/system/role', 'system/role/index', 1, NULL, NULL, 1, NULL, NULL, 2.00, 0, 'ant-design:solution', 0, 1, 0, NULL, NULL, NULL, '2018-12-25 20:34:38', 'admin', '2021-09-17 15:58:00', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1912753560201089025', '1892553163993931777', 'OCR识别', '/ai/ocr', 'super/airag/ocr/AiOcrList', 1, '', NULL, 1, NULL, '0', 6.00, 0, 'ant-design:scan-outlined', 1, 0, 0, 0, NULL, 'admin', '2025-04-17 14:22:41', 'admin', '2025-06-26 11:14:58', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1930221213607591937', '1893865471550578689', '新增或编辑AI应用', NULL, NULL, 0, NULL, NULL, 2, 'airag:app:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:12:54', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930221335938662401', '1893865471550578689', '删除AI应用', NULL, NULL, 0, NULL, NULL, 2, 'airag:app:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:13:23', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930221570324758530', '1892557342028226561', '添加AI知识库', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:14:19', 'admin', '2025-06-04 19:21:38', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930221637551063042', '1892557342028226561', '编辑AI知识库', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:14:35', 'admin', '2025-06-04 19:21:42', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930221702164316161', '1892557342028226561', '重建AI知识库', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:rebuild', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:14:50', 'admin', '2025-06-04 19:21:46', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930221774230847490', '1892557342028226561', '删除AI知识库', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:15:07', 'admin', '2025-06-04 19:21:52', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930221983555977217', '1892557342028226561', '新增编辑AI知识库文档', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:doc:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:15:57', 'admin', '2025-06-04 19:22:03', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222066120851457', '1892557342028226561', '导入AI知识库文档', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:doc:zip', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:16:17', 'admin', '2025-06-04 19:22:09', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222218734796802', '1892557342028226561', '向量化AI知识库文档', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:doc:rebuild', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:16:53', 'admin', '2025-06-04 19:22:16', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222295012409345', '1892557342028226561', '批量删除AI知识库文档', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:doc:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:17:12', 'admin', '2025-06-04 19:22:21', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222395180777474', '1892557342028226561', '清空AI知识库文档', NULL, NULL, 0, NULL, NULL, 2, 'airag:knowledge:doc:deleteAll', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:17:35', 'admin', '2025-06-04 19:22:25', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222558582472705', '1892553778493022209', '新增AI模型', NULL, NULL, 0, NULL, NULL, 2, 'airag:model:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:18:14', 'admin', '2025-06-04 19:21:16', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222617197871105', '1892553778493022209', '编辑AI模型', NULL, NULL, 0, NULL, NULL, 2, 'airag:model:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:18:28', 'admin', '2025-06-04 19:21:20', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222679269376001', '1892553778493022209', '删除AI模型', NULL, NULL, 0, NULL, NULL, 2, 'airag:model:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:18:43', 'admin', '2025-06-04 19:21:24', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222862556266498', '1890213291321749505', '新增AI流程', NULL, NULL, 0, NULL, NULL, 2, 'airag:flow:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:19:27', 'admin', '2025-06-04 19:21:08', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930222953853681666', '1890213291321749505', '编辑AI流程', NULL, NULL, 0, NULL, NULL, 2, 'airag:flow:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:19:49', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930223034757611522', '1890213291321749505', '保存AI流程设计', NULL, NULL, 0, NULL, NULL, 2, 'airag:flow:designSave', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:20:08', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930223114757611522', '1890213291321749505', 'AI流程测试', NULL, NULL, 0, NULL, NULL, 2, 'airag:flow:debug', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-12-01 19:20:08', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1930223132619112449', '1890213291321749505', '删除AI流程', NULL, NULL, 0, NULL, NULL, 2, 'airag:flow:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-06-04 19:20:31', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1939572818833301506', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '首页配置', '/system/homeConfig', 'system/homeConfig/index', 1, '', NULL, 1, NULL, '0', 1.00, 0, 'ant-design:appstore-outlined', 0, 0, 0, 0, NULL, 'admin', '2025-06-30 14:32:50', 'admin', '2025-07-01 20:13:22', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1941349246536998913', '1939572818833301506', '首页配置-添加', NULL, NULL, 0, NULL, NULL, 2, 'system:roleindex:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-07-05 12:11:44', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1941349335431077889', '1939572818833301506', '首页配置-编辑', NULL, NULL, 0, NULL, NULL, 2, 'system:roleindex:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-07-05 12:12:05', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1941349462887587842', '1939572818833301506', '首页配置-删除', NULL, NULL, 0, NULL, NULL, 2, 'system:roleindex:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-07-05 12:12:35', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1941349550087168001', '1939572818833301506', '首页配置-批量删除', NULL, NULL, 0, NULL, NULL, 2, 'system:roleindex:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-07-05 12:12:56', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1947833384695164929', '1629109281748291586', '第三方配置删除', NULL, NULL, 0, NULL, NULL, 2, 'system:third:config:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-07-23 09:37:23', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1960994076329316353', '119213522910765570', '添加一个用户和多个套餐关系', NULL, NULL, 0, NULL, NULL, 2, 'system:tenant:addPacksUser', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-08-28 17:13:16', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1961009998209257473', '1674708136602542082', '租户部门', '/depart/TenantDepartList', 'system/depart/TenantDepartList', 1, '', NULL, 1, NULL, '0', 3.30, 0, 'ant-design:apartment-outlined', 0, 0, 0, 0, NULL, 'admin', '2025-08-28 18:16:32', 'admin', '2025-08-29 10:20:25', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1961253156897710081', '1674708136602542082', '租户套餐', '/pack/TenantCurrentPackList', 'system/tenant/pack/TenantCurrentPackList', 1, '', NULL, 1, NULL, '0', 3.40, 0, 'ant-design:read-filled', 0, 0, 0, 0, NULL, 'admin', '2025-08-29 10:22:46', 'admin', '2025-08-29 10:24:46', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1963086454217281537', '1674708136602542082', '租户职务', '/position/TenantPositionList', 'system/position/TenantPositionList', 1, '', NULL, 1, NULL, '0', 3.50, 0, 'ant-design:user-outlined', 1, 0, 0, 0, NULL, 'admin', '2025-09-03 11:47:38', NULL, NULL, 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1963133393868722178', '1674708136602542082', '我的租户', '/my/MyTenantDetail', 'system/tenant/my/MyTenantDetail', 1, '', NULL, 1, NULL, '0', 0.00, 0, 'ant-design:user-outlined', 1, 0, 0, 0, NULL, 'admin', '2025-09-03 14:54:09', 'admin', '2025-09-13 17:16:57', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1972617196420993025', '45c966826eeff4c99b8f8ebfe74511fc', '部门管理拖拽修改上下级', NULL, NULL, 0, NULL, NULL, 2, 'system:depart:updateChange', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-09-29 18:59:24', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1972645086223814657', '3f915b2769fc80648e92d04e84ca059d', '重置系统密码', NULL, NULL, 0, NULL, NULL, 2, 'system:user:resetPassword', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-09-29 20:50:13', 'admin', '2025-09-30 11:58:29', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('1980223355087781889', '1892553163993931777', 'MCP配置', '/super/airag/aimcp/AiragMcpList', 'super/airag/aimcp/AiragMcpList', 1, '', NULL, 1, NULL, '0', 5.00, 0, 'ant-design:tool-twotone', 1, 0, 0, 0, NULL, 'admin', '2025-10-20 18:43:33', 'admin', '2025-10-21 19:00:31', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('1a0811914300741f4e11838ff37a1d3a', '3f915b2769fc80648e92d04e84ca059d', '手机号禁用', '', '', 0, NULL, NULL, 2, 'user:form:phone', '2', 1.00, 0, NULL, 1, NULL, 0, NULL, NULL, 'admin', '2019-05-11 17:19:30', 'admin', '2019-05-11 18:00:22', 0, 0, '1', NULL);
INSERT INTO `sys_permission` VALUES ('1d592115213910765570', '3f915b2769fc80648e92d04e84ca059d', '通过ID查询用户拥有的角色', NULL, NULL, 0, NULL, NULL, 2, 'system:user:queryUserRole', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2022-11-14 19:20:22', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050104193350031', '2025050104193340030', '添加接口管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 16:19:03', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050104193350032', '2025050104193340030', '编辑接口管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 16:19:03', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050104193350033', '2025050104193340030', '删除接口管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 16:19:03', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050104193350034', '2025050104193340030', '批量删除接口管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 16:19:03', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050104193350035', '2025050104193340030', '导出excel_接口管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 16:19:03', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050104193350036', '2025050104193340030', '导入excel_接口管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 16:19:03', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050105554940201', '2025050105554940200', '添加授权管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api_auth:add', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 17:55:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050105554940202', '2025050105554940200', '编辑授权管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api_auth:edit', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 17:55:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050105554940203', '2025050105554940200', '删除授权管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api_auth:delete', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 17:55:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050105554940204', '2025050105554940200', '批量删除授权管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api_auth:deleteBatch', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 17:55:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050105554940205', '2025050105554940200', '导出excel_授权管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api_auth:exportXls', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 17:55:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('2025050105554940206', '2025050105554940200', '导入excel_授权管理', NULL, NULL, 0, NULL, NULL, 2, 'openapi:open_api_auth:importExcel', '1', NULL, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2025-05-01 17:55:20', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('3f915b2769fc80648e92d04e84ca059d', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '用户管理', '/system/user', 'system/user/index', 1, NULL, NULL, 1, NULL, NULL, 1.00, 0, 'ant-design:user', 0, 1, 0, NULL, NULL, NULL, '2018-12-25 20:34:38', 'sunjianlei', '2021-05-08 09:57:31', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('45c966826eeff4c99b8f8ebfe74511fc', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '部门管理', '/system/depart', 'system/depart/index', 1, NULL, NULL, 1, NULL, NULL, 3.00, 0, 'ant-design:team', 0, 0, 0, NULL, NULL, 'admin', '2019-01-29 18:47:40', 'admin', '2021-09-17 15:58:13', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('5c2f42277948043026b7a14692456828', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '我的部门', '/system/depart-user', 'system/departUser/index', 1, NULL, NULL, 1, NULL, NULL, 3.00, 0, 'ant-design:home-outlined', 0, 0, 0, 0, NULL, 'admin', '2019-04-17 15:12:24', 'admin', '2023-03-04 15:03:07', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('9502685863ab87f0ad1134142788a385', '1438108176273760258', '首页', '/dashboard/analysis', 'dashboard/Analysis', 1, NULL, NULL, 1, NULL, NULL, 1.00, 0, 'ant-design:qrcode-outlined', 1, 1, 0, 0, NULL, NULL, '2018-12-25 20:34:38', 'jeecg', '2024-06-18 23:09:37', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_access_settings', '', '接入设置', '/accessSettings', 'layouts/RouteView', 1, NULL, NULL, 0, NULL, '1', 2.40, 0, 'ant-design:setting-outlined', 0, 0, 0, 0, '接入设置', 'admin', '2026-02-11 18:16:16', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_agent', 'cs_parent', '客服管理', '/cs/agent', 'super/airag/cs/agent/index', 1, NULL, NULL, 1, NULL, '1', 2.00, 0, 'ant-design:team-outlined', 1, 0, 0, 0, '客服人员管理', 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_agent_add', 'cs_agent', '添加客服', NULL, NULL, 0, NULL, NULL, 2, 'cs:agent:add', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_agent_delete', 'cs_agent', '删除客服', NULL, NULL, 0, NULL, NULL, 2, 'cs:agent:delete', '1', 3.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_agent_edit', 'cs_agent', '编辑客服', NULL, NULL, 0, NULL, NULL, 2, 'cs:agent:edit', '1', 2.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_agent_ip_whitelist', 'cs_security', '客服IP白名单', '/security/agentIpWhitelist', 'super/airag/cs/security/agentIpWhitelist/index', 1, NULL, NULL, 1, NULL, '1', 3.00, 0, 'ant-design:check-circle-outlined', 0, 0, 0, 0, NULL, 'admin', '2026-02-09 16:44:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_agent_login_log', 'cs_security', '客服登录日志', '/security/loginLog', 'super/airag/cs/security/loginLog/index', 1, NULL, NULL, 1, NULL, '1', 4.00, 0, 'ant-design:file-text-outlined', 0, 0, 0, 0, NULL, 'admin', '2026-02-09 16:44:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_auto_message', 'cs_parent', '自动消息', '/cs/autoMessage', 'super/airag/cs/autoMessage/index', 1, NULL, NULL, 1, NULL, NULL, 5.00, NULL, 'ant-design:notification-outlined', 1, 0, 0, 0, '自动消息配置', 'admin', '2026-02-06 17:10:58', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_brand_config', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '品牌配置', '/cs/brand', 'super/airag/cs/brand/index', 1, NULL, NULL, 1, NULL, '1', 7.00, 0, 'ant-design:highlight-outlined', 0, 0, 0, 0, '客服系统品牌配置', 'admin', '2026-01-20 15:08:49', 'admin', '2026-01-20 15:09:30', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_brand_save', 'cs_brand_config', '保存品牌配置', NULL, NULL, 0, NULL, NULL, 2, 'cs:brand:save', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-20 15:08:49', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_chat_window_settings', 'cs_access_settings', '聊天窗口设置', '/accessSettings/chatWindowSettings', 'super/airag/cs/chatWindowSettings/index', 1, NULL, NULL, 1, NULL, '1', 1.00, 0, 'ant-design:message-outlined', 0, 0, 0, 0, '聊天窗口设置', 'admin', '2026-02-11 18:16:16', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_conversation', 'cs_parent', '会话记录', '/cs/conversation', 'super/airag/cs/conversation/index', 1, NULL, NULL, 1, NULL, '1', 3.00, 0, 'ant-design:history-outlined', 1, 0, 0, 0, '会话记录查询', 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_conversation_assign', 'cs_parent', '对话分配', '/cs/conversationAssign', 'super/airag/cs/conversationAssign/index', 1, NULL, NULL, 1, NULL, NULL, 4.00, NULL, 'ant-design:swap-outlined', 0, 0, 0, 0, '对话分配配置', 'admin', '2026-02-06 15:09:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_conversation_export', 'cs_conversation', '导出会话', NULL, NULL, 0, NULL, NULL, 2, 'cs:conversation:export', '1', 2.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_conversation_view', 'cs_conversation', '查看会话', NULL, NULL, 0, NULL, NULL, 2, 'cs:conversation:view', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_ip_blacklist', 'cs_security', '访客IP黑名单', '/security/ipBlacklist', 'super/airag/cs/security/ipBlacklist/index', 1, NULL, NULL, 1, NULL, '1', 1.00, 0, 'ant-design:stop-outlined', 0, 0, 0, 0, NULL, 'admin', '2026-02-09 16:44:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_message_board_records', 'cs_parent', '留言记录', '/cs/messageBoardRecords', 'super/airag/cs/messageBoard/records', 1, NULL, NULL, 1, NULL, NULL, 9.00, NULL, 'ant-design:file-text-outlined', 0, 0, 0, 0, '留言记录管理', 'admin', '2026-02-06 15:09:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_message_board_settings', 'cs_parent', '留言板设置', '/cs/messageBoardSettings', 'super/airag/cs/messageBoard/settings', 1, NULL, NULL, 1, NULL, NULL, 8.00, NULL, 'ant-design:form-outlined', 0, 0, 0, 0, '留言板设置', 'admin', '2026-02-06 15:09:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_parent', '', '在线客服', '/cs', 'layouts/RouteView', 1, NULL, NULL, 0, NULL, '1', 1.00, 0, 'ant-design:customer-service-outlined', 0, 0, 0, 0, '在线客服管理', 'admin', '2026-01-07 17:22:52', 'admin', '2026-01-21 08:53:18', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_quick_access', 'cs_access_settings', '快速接入', '/accessSettings/quickAccess', 'super/airag/cs/accessExample/index', 1, NULL, NULL, 1, NULL, '1', 3.00, 0, 'ant-design:rocket-outlined', 0, 0, 0, 0, '快速接入-第三方接入示例', 'admin', '2026-02-11 22:24:59', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_quick_reply', 'cs_parent', '快捷回复', '/cs/quickReply', 'super/airag/cs/quickReply/index', 1, NULL, NULL, 1, NULL, '1', 5.00, 0, 'ant-design:thunderbolt-outlined', 1, 0, 0, 0, '快捷回复管理', 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_quick_reply_add', 'cs_quick_reply', '添加快捷回复', NULL, NULL, 0, NULL, NULL, 2, 'cs:quickReply:add', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_quick_reply_delete', 'cs_quick_reply', '删除快捷回复', NULL, NULL, 0, NULL, NULL, 2, 'cs:quickReply:delete', '1', 3.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_quick_reply_edit', 'cs_quick_reply', '编辑快捷回复', NULL, NULL, 0, NULL, NULL, 2, 'cs:quickReply:edit', '1', 2.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_security', '', '安全设置', '/security', 'layouts/RouteView', 1, NULL, NULL, 0, NULL, '1', 2.30, 0, 'ant-design:safety-certificate-outlined', 0, 0, 0, 0, NULL, 'admin', '2026-02-09 16:44:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_sensitive_words', 'cs_access_settings', '访客敏感词', '/accessSettings/sensitiveWords', 'super/airag/cs/sensitiveWords/index', 1, NULL, NULL, 1, NULL, '1', 2.00, 0, 'ant-design:warning-outlined', 0, 0, 0, 0, '访客敏感词配置', 'admin', '2026-02-11 18:17:26', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_sub_agent', 'cs_parent', '团队管理', '/cs/subAgent', 'super/airag/cs/subAgent/index', 1, NULL, NULL, 1, NULL, '1', 2.50, 0, 'ant-design:usergroup-add-outlined', 1, 0, 0, 0, '子客服管理', 'admin', '2026-02-09 11:42:38', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_visitor', 'airag_cs', '访客管理', '/super/airag/cs/visitor', 'super/airag/cs/visitor/index', 1, NULL, NULL, 1, NULL, '1', 5.00, 0, 'ant-design:user-outlined', 1, 0, 0, 0, '访客信息管理', 'admin', '2026-01-12 17:13:12', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_visitor_blacklist', 'cs_security', '访客黑名单', '/security/visitorBlacklist', 'super/airag/cs/security/visitorBlacklist/index', 1, NULL, NULL, 1, NULL, '1', 2.00, 0, 'ant-design:user-delete-outlined', 0, 0, 0, 0, NULL, 'admin', '2026-02-09 16:44:00', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_workbench', 'cs_parent', '客服工作台', '/cs/workbench', 'super/airag/cs/workbench/index', 1, NULL, NULL, 1, NULL, '1', 1.00, 0, 'ant-design:message-outlined', 1, 0, 0, 0, '客服工作台', 'admin', '2026-01-07 17:22:52', NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('d7d6e2e4e2934f2c9385a623fd98c6f3', '', '系统管理', '/isystem', 'layouts/RouteView', 1, NULL, NULL, 0, NULL, NULL, 4.00, 0, 'ant-design:setting', 0, 0, 0, 0, NULL, NULL, '2018-12-25 20:34:38', 'admin', '2025-06-25 14:24:07', 0, 0, NULL, 0);
INSERT INTO `sys_permission` VALUES ('f15543b0263cf6c5fac85afdd3eba3f2', '3f915b2769fc80648e92d04e84ca059d', '用户导入', '', NULL, 0, NULL, NULL, 2, 'system:user:import', '1', 1.00, 0, NULL, 1, 0, 0, 0, NULL, 'admin', '2019-05-13 19:15:27', 'admin', '2022-06-30 15:05:12', 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('sys_license_status', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '授权信息', '/isystem/licenseStatus', 'system/license/LicenseStatus', 1, NULL, NULL, 1, NULL, '1', 10.00, 0, 'ant-design:safety-certificate-outlined', 1, 0, 0, 0, '查看当前系统授权状态、配额、功能列表', 'admin', '2026-02-25 15:59:09', NULL, NULL, 0, 0, '1', 0);

-- ----------------------------
-- Table structure for sys_permission_data_rule
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission_data_rule`;
CREATE TABLE `sys_permission_data_rule`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'ID',
  `permission_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '菜单ID',
  `rule_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '规则名称',
  `rule_column` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字段',
  `rule_conditions` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '条件',
  `rule_value` varchar(300) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '规则值',
  `status` varchar(3) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限有效状态1有0否',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_spdr_permission_id`(`permission_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_permission_data_rule
-- ----------------------------
INSERT INTO `sys_permission_data_rule` VALUES ('1260935285157511170', '4148ec82b6acd69f470bea75fe41c357', 'createBy', 'createBy', '=', '#{sys_user_code}', '0', '2020-05-14 22:09:34', 'jeecg', '2020-05-14 22:13:52', 'admin');
INSERT INTO `sys_permission_data_rule` VALUES ('1260936345293012993', '4148ec82b6acd69f470bea75fe41c357', '年龄', 'age', '>', '20', '1', '2020-05-14 22:13:46', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('1260937192290762754', '4148ec82b6acd69f470bea75fe41c357', 'sysOrgCode', 'sysOrgCode', 'RIGHT_LIKE', '#{sys_org_code}', '1', '2020-05-14 22:17:08', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('32b62cb04d6c788d9d92e3ff5e66854e', '8d4683aacaa997ab86b966b464360338', '000', '00', '!=', '00', '1', '2019-04-02 18:36:08', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('40283181614231d401614234fe670003', '40283181614231d401614232cd1c0001', 'createBy', 'createBy', '=', '#{sys_user_code}', '1', '2018-01-29 21:57:04', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('4028318161424e730161424fca6f0004', '4028318161424e730161424f61510002', 'createBy', 'createBy', '=', '#{sys_user_code}', '1', '2018-01-29 22:26:20', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402880e6487e661a01487e732c020005', '402889fb486e848101486e93a7c80014', 'SYS_ORG_CODE', 'SYS_ORG_CODE', 'LIKE', '010201%', '1', '2014-09-16 20:32:30', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402880e6487e661a01487e8153ee0007', '402889fb486e848101486e93a7c80014', 'create_by', 'create_by', '', '#{SYS_USER_CODE}', '1', '2014-09-16 20:47:57', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402880ec5ddec439015ddf9225060038', '40288088481d019401481d2fcebf000d', '复杂关系', '', 'USE_SQL_RULES', 'name like \'%张%\' or age > 10', '1', NULL, NULL, '2017-08-14 15:10:25', 'demo');
INSERT INTO `sys_permission_data_rule` VALUES ('402880ec5ddfdd26015ddfe3e0570011', '4028ab775dca0d1b015dca3fccb60016', '复杂sql配置', '', 'USE_SQL_RULES', 'table_name like \'%test%\' or is_tree = \'Y\'', '1', NULL, NULL, '2017-08-14 16:38:55', 'demo');
INSERT INTO `sys_permission_data_rule` VALUES ('402880f25b1e2ac7015b1e5fdebc0012', '402880f25b1e2ac7015b1e5cdc340010', '只能看自己数据', 'create_by', '=', '#{sys_user_code}', '1', '2017-03-30 16:40:51', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402881875b19f141015b19f8125e0014', '40288088481d019401481d2fcebf000d', '可看下属业务数据', 'sys_org_code', 'LIKE', '#{sys_org_code}', '1', NULL, NULL, '2017-08-14 15:04:32', 'demo');
INSERT INTO `sys_permission_data_rule` VALUES ('402881e45394d66901539500a4450001', '402881e54df73c73014df75ab670000f', 'sysCompanyCode', 'sysCompanyCode', '=', '#{SYS_COMPANY_CODE}', '1', '2016-03-21 01:09:21', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402881e45394d6690153950177cb0003', '402881e54df73c73014df75ab670000f', 'sysOrgCode', 'sysOrgCode', '=', '#{SYS_ORG_CODE}', '1', '2016-03-21 01:10:15', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402881e56266f43101626727aff60067', '402881e56266f43101626724eb730065', '销售自己看自己的数据', 'createBy', '=', '#{sys_user_code}', '1', '2018-03-27 19:11:16', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402881e56266f4310162672fb1a70082', '402881e56266f43101626724eb730065', '销售经理看所有下级数据', 'sysOrgCode', 'LIKE', '#{sys_org_code}', '1', '2018-03-27 19:20:01', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402881e56266f431016267387c9f0088', '402881e56266f43101626724eb730065', '只看金额大于1000的数据', 'money', '>=', '1000', '1', '2018-03-27 19:29:37', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402881f3650de25101650dfb5a3a0010', '402881e56266f4310162671d62050044', '22', '', 'USE_SQL_RULES', '22', '1', '2018-08-06 14:45:01', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402889fb486e848101486e913cd6000b', '402889fb486e848101486e8e2e8b0007', 'userName', 'userName', '=', 'admin', '1', '2014-09-13 18:31:25', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('402889fb486e848101486e98d20d0016', '402889fb486e848101486e93a7c80014', 'title', 'title', '=', '12', '1', NULL, NULL, '2014-09-13 22:18:22', 'scott');
INSERT INTO `sys_permission_data_rule` VALUES ('402889fe47fcb29c0147fcb6b6220001', '8a8ab0b246dc81120146dc8180fe002b', '12', '12', '>', '12', '1', '2014-08-22 15:55:38', '8a8ab0b246dc81120146dc8181950052', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('4028ab775dca0d1b015dca4183530018', '4028ab775dca0d1b015dca3fccb60016', '表名限制', 'isDbSynch', '=', 'Y', '1', NULL, NULL, '2017-08-14 16:43:45', 'demo');
INSERT INTO `sys_permission_data_rule` VALUES ('4028ef815595a881015595b0ccb60001', '40288088481d019401481d2fcebf000d', '限只能看自己', 'create_by', '=', '#{sys_user_code}', '1', NULL, NULL, '2017-08-14 15:03:56', 'demo');
INSERT INTO `sys_permission_data_rule` VALUES ('4028ef81574ae99701574aed26530005', '4028ef81574ae99701574aeb97bd0003', '用户名', 'userName', '!=', 'admin', '1', '2016-09-21 12:07:18', 'admin', NULL, NULL);
INSERT INTO `sys_permission_data_rule` VALUES ('f852d85d47f224990147f2284c0c0005', NULL, '小于', 'test', '<=', '11', '1', '2014-08-20 14:43:52', '8a8ab0b246dc81120146dc8181950052', NULL, NULL);

-- ----------------------------
-- Table structure for sys_position
-- ----------------------------
DROP TABLE IF EXISTS `sys_position`;
CREATE TABLE `sys_position`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `code` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '职务编码',
  `name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '职务级别名称',
  `post_level` int NULL DEFAULT NULL COMMENT '职务等级',
  `company_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '公司id',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `sys_org_code` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '组织机构编码',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '职务级别' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_position
-- ----------------------------
INSERT INTO `sys_position` VALUES ('1958470823064436737', '5za8WqucKR', '职员', 6, NULL, 'admin', '2025-08-21 18:06:46', NULL, NULL, 'A01A08', 0);
INSERT INTO `sys_position` VALUES ('1958470865577902082', 'hGAuYslALj', '副部长', 5, NULL, 'admin', '2025-08-21 18:06:56', NULL, NULL, 'A01A08', 0);
INSERT INTO `sys_position` VALUES ('1958470912214368258', 'M0xkqpPsg7', '部长', 4, NULL, 'admin', '2025-08-21 18:07:07', NULL, NULL, 'A01A08', 0);
INSERT INTO `sys_position` VALUES ('1958471030867034113', 'npEbkFq6Uw', '副总经理', 3, NULL, 'admin', '2025-08-21 18:07:35', NULL, NULL, 'A01A08', 0);
INSERT INTO `sys_position` VALUES ('1958471074953363458', 'DEPMkWRJEu', '总经理', 2, NULL, 'admin', '2025-08-21 18:07:46', 'admin', '2025-09-13 18:52:25', 'A01A08', 0);
INSERT INTO `sys_position` VALUES ('1958471111989067778', 'gu7Rbffh0L', '董事长', 1, NULL, 'admin', '2025-08-21 18:07:54', NULL, NULL, 'A01A08', 0);

-- ----------------------------
-- Table structure for sys_quartz_job
-- ----------------------------
DROP TABLE IF EXISTS `sys_quartz_job`;
CREATE TABLE `sys_quartz_job`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `del_flag` int NULL DEFAULT NULL COMMENT '删除状态',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `job_class_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '任务类名',
  `cron_expression` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'cron表达式',
  `parameter` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '参数',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `status` int NULL DEFAULT NULL COMMENT '状态 0正常 -1停止',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_quartz_job
-- ----------------------------
INSERT INTO `sys_quartz_job` VALUES ('1966781755167879169', 'admin', '2025-09-13 16:31:26', 0, NULL, NULL, 'org.jeecg.modules.system.job.UserUpadtePwdJob', '0 0 0 * * ? *', NULL, '5个月未修改密码提醒', 0);
INSERT INTO `sys_quartz_job` VALUES ('5b3d2c087ad41aa755fc4f89697b01e7', 'admin', '2019-04-11 19:04:21', 0, 'admin', '2024-03-18 14:21:35', 'org.jeecg.modules.message.job.SendMsgJob', '0/50 * * * * ? *', NULL, NULL, -1);
INSERT INTO `sys_quartz_job` VALUES ('a253cdfc811d69fa0efc70d052bc8128', 'admin', '2019-03-30 12:44:48', 0, 'admin', '2020-05-02 15:48:49', 'org.jeecg.modules.quartz.job.SampleJob', '0/1 * * * * ?', NULL, NULL, -1);
INSERT INTO `sys_quartz_job` VALUES ('df26ecacf0f75d219d746750fe84bbee', NULL, '2021-06-30 16:03:09', 0, 'admin', '2020-05-02 15:40:35', 'org.jeecg.modules.quartz.job.SampleParamJob', '0/1 * * * * ?', 'scott', '带参测试 后台将每隔1秒执行输出日志', -1);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `role_name` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色名称',
  `role_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '角色编码',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_sys_role_role_code`(`role_code` ASC) USING BTREE,
  INDEX `idx_sysrole_tenant_id`(`tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES ('cs_admin_agent_role_001', '管理员客服', 'cs_admin_agent', '管理员客服，拥有完整在线客服权限及团队管理', 'admin', '2026-02-09 14:44:37', NULL, NULL, 0);
INSERT INTO `sys_role` VALUES ('cs_sub_agent_role_001', '子客服', 'cs_sub_agent', '管理员客服创建的子客服角色', 'admin', '2026-02-09 11:42:38', NULL, NULL, 0);
INSERT INTO `sys_role` VALUES ('f6817f48af4fb3af11b9e8bf182f618b', '管理员', 'admin', '管理员', NULL, '2020-12-21 18:03:39', 'admin', '2025-07-30 15:17:55', 0);

-- ----------------------------
-- Table structure for sys_role_index
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_index`;
CREATE TABLE `sys_role_index`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色编码',
  `url` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '组件',
  `is_route` tinyint(1) NULL DEFAULT 1 COMMENT '是否路由菜单: 0:不是  1:是（默认值1）',
  `priority` int NULL DEFAULT 0 COMMENT '优先级',
  `status` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '状态0:无效 1:有效',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `sys_org_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属部门',
  `relation_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联关系(ROLE:角色 USER:用户)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sri_role_code`(`role_code` ASC) USING BTREE,
  INDEX `idx_sri_status`(`status` ASC) USING BTREE,
  INDEX `idx_sri_priority`(`priority` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色首页表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_index
-- ----------------------------
INSERT INTO `sys_role_index` VALUES ('1803082647166652418', 'DEF_INDEX_ALL', '/cs/workbench', 'super/airag/cs/workbench/index', 1, 0, '0', 'jeecg', '2024-06-18 23:09:37', 'admin', '2026-01-21 08:53:32', 'A02A01', 'DEFAULT');

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `role_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色id',
  `permission_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '权限id',
  `data_rule_ids` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据权限ids',
  `operate_date` datetime NULL DEFAULT NULL COMMENT '操作时间',
  `operate_ip` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '操作ip',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_srp_role_per_id`(`role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `idx_srp_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_srp_permission_id`(`permission_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '角色权限表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_role_permission
-- ----------------------------
INSERT INTO `sys_role_permission` VALUES ('00b0748f04d3ea52c8cfa179c1c9d384', '52b0cf022ac4187b2a70dfa4f8b2d940', 'd7d6e2e4e2934f2c9385a623fd98c6f3', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('115a6673ae6c0816d3f60de221520274', '21c5a3187763729408b40afb0d0fdfa8', '63b551e81c5956d5c861593d366d8c57', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('1209423580355481602', 'f6817f48af4fb3af11b9e8bf182f618b', '190c2b43bec6a5f7a4194a85db67d96a', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('1580861542625955842', 'f6817f48af4fb3af11b9e8bf182f618b', '1170592628746878978', NULL, '2022-10-14 18:02:17', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1592114400605859842', 'f6817f48af4fb3af11b9e8bf182f618b', '1592113148350263298', NULL, '2022-11-14 19:17:08', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473537', 'f6817f48af4fb3af11b9e8bf182f618b', '1588513553652436993', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473538', 'f6817f48af4fb3af11b9e8bf182f618b', '1592114574275211265', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473539', 'f6817f48af4fb3af11b9e8bf182f618b', '1592114652566089729', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473540', 'f6817f48af4fb3af11b9e8bf182f618b', '1592115213910765570', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473541', 'f6817f48af4fb3af11b9e8bf182f618b', '1592115162379546625', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473542', 'f6817f48af4fb3af11b9e8bf182f618b', '1592115115361398786', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473543', 'f6817f48af4fb3af11b9e8bf182f618b', '1592115070432014338', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473544', 'f6817f48af4fb3af11b9e8bf182f618b', '1592114955650691074', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473545', 'f6817f48af4fb3af11b9e8bf182f618b', '1592114893302362114', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473546', 'f6817f48af4fb3af11b9e8bf182f618b', '1592114823467200514', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006353473547', 'f6817f48af4fb3af11b9e8bf182f618b', '1592114772665790465', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388100', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120323667750914', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388101', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120372296511490', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388102', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120427007012865', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388103', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120484120850434', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388104', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120594695286785', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388105', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120649007329281', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388106', 'f6817f48af4fb3af11b9e8bf182f618b', '1592112984361365505', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388107', 'f6817f48af4fb3af11b9e8bf182f618b', '1592115914493751297', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388108', 'f6817f48af4fb3af11b9e8bf182f618b', '1592116663936184322', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388109', 'f6817f48af4fb3af11b9e8bf182f618b', '1592118604640645122', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388110', 'f6817f48af4fb3af11b9e8bf182f618b', '1592118648315932674', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388111', 'f6817f48af4fb3af11b9e8bf182f618b', '1592119001883176961', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388112', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120052866707457', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388119', 'f6817f48af4fb3af11b9e8bf182f618b', '1592118053634928641', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388120', 'f6817f48af4fb3af11b9e8bf182f618b', '1592117990305132545', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388121', 'f6817f48af4fb3af11b9e8bf182f618b', '1592117804359053314', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388122', 'f6817f48af4fb3af11b9e8bf182f618b', '1592117748209905665', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1593153006416388123', 'f6817f48af4fb3af11b9e8bf182f618b', '1592117625664925697', NULL, '2022-11-17 16:04:11', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1693199779486212099', 'f6817f48af4fb3af11b9e8bf182f618b', '1592120224120850434', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779490406401', 'f6817f48af4fb3af11b9e8bf182f618b', '1592135223910765570', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779502989320', 'f6817f48af4fb3af11b9e8bf182f618b', '1593185714482880514', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779519766533', 'f6817f48af4fb3af11b9e8bf182f618b', '1693195557097164801', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779519766535', 'f6817f48af4fb3af11b9e8bf182f618b', '15c92115213910765570', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779519766536', 'f6817f48af4fb3af11b9e8bf182f618b', '1d592115213910765570', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779549126660', 'f6817f48af4fb3af11b9e8bf182f618b', '1211885237487923202', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779549126661', 'f6817f48af4fb3af11b9e8bf182f618b', '1214376304951664642', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779549126662', 'f6817f48af4fb3af11b9e8bf182f618b', '1214462306546319362', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779570098187', 'f6817f48af4fb3af11b9e8bf182f618b', '1a0811914300741f4e11838ff37a1d3a', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779586875405', 'f6817f48af4fb3af11b9e8bf182f618b', '119213522910765570', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1693199779591069698', 'f6817f48af4fb3af11b9e8bf182f618b', '1597419994965786625', NULL, '2023-08-20 17:54:20', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1765276566625693698', 'f6817f48af4fb3af11b9e8bf182f618b', '1750128461040648193', NULL, '2024-03-06 15:21:45', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('1765276566634082305', 'f6817f48af4fb3af11b9e8bf182f618b', '1592114721138765826', NULL, '2024-03-06 15:21:45', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('17ead5b7d97ed365398ab20009a69ea3', '52b0cf022ac4187b2a70dfa4f8b2d940', 'e08cb190ef230d5d4f03824198773950', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('1892117657990971394', '1456165677820301314', '1867047795019440130', NULL, '2025-02-19 15:42:58', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1892117657990971395', '1456165677820301314', '1867041505346019330', NULL, '2025-02-19 15:42:58', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1907407238864781314', 'f6817f48af4fb3af11b9e8bf182f618b', '1892553163993931777', NULL, '2025-04-02 20:18:18', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1907407238864781315', 'f6817f48af4fb3af11b9e8bf182f618b', '1895401981290643458', NULL, '2025-04-02 20:18:18', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1907407238864781316', 'f6817f48af4fb3af11b9e8bf182f618b', '1892553778493022209', NULL, '2025-04-02 20:18:18', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1907407238864781317', 'f6817f48af4fb3af11b9e8bf182f618b', '1892557342028226561', NULL, '2025-04-02 20:18:18', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1907407238864781318', 'f6817f48af4fb3af11b9e8bf182f618b', '1893865471550578689', NULL, '2025-04-02 20:18:18', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1923218122706386946', 'f6817f48af4fb3af11b9e8bf182f618b', '1890213291321749505', NULL, '2025-05-16 11:25:07', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1993532089659559937', 'f6817f48af4fb3af11b9e8bf182f618b', '1980223355087781889', NULL, '2025-11-26 12:07:43', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1993532089659559938', 'f6817f48af4fb3af11b9e8bf182f618b', '1912753560201089025', NULL, '2025-11-26 12:07:43', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1993532463099416578', 'f6817f48af4fb3af11b9e8bf182f618b', '1972645086223814657', NULL, '2025-11-26 12:09:12', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279299293186', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222862556266498', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279299293187', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222953853681666', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279299293188', 'f6817f48af4fb3af11b9e8bf182f618b', '1930223034757611522', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279299293189', 'f6817f48af4fb3af11b9e8bf182f618b', '1930223114757611522', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279299293190', 'f6817f48af4fb3af11b9e8bf182f618b', '1930223132619112449', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207745', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222295012409345', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207746', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222395180777474', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207747', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222218734796802', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207748', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222066120851457', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207749', 'f6817f48af4fb3af11b9e8bf182f618b', '1930221983555977217', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207750', 'f6817f48af4fb3af11b9e8bf182f618b', '1930221774230847490', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207751', 'f6817f48af4fb3af11b9e8bf182f618b', '1930221702164316161', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207752', 'f6817f48af4fb3af11b9e8bf182f618b', '1930221637551063042', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391279362207753', 'f6817f48af4fb3af11b9e8bf182f618b', '1930221570324758530', NULL, '2025-12-01 15:15:28', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391337201659906', 'f6817f48af4fb3af11b9e8bf182f618b', '1930221213607591937', NULL, '2025-12-01 15:15:42', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391337201659907', 'f6817f48af4fb3af11b9e8bf182f618b', '1930221335938662401', NULL, '2025-12-01 15:15:42', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391337201659908', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222679269376001', NULL, '2025-12-01 15:15:42', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391337201659909', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222617197871105', NULL, '2025-12-01 15:15:42', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1995391337201659910', 'f6817f48af4fb3af11b9e8bf182f618b', '1930222558582472705', NULL, '2025-12-01 15:15:42', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('1ac1688ef8456f384091a03d88a89ab1', '52b0cf022ac4187b2a70dfa4f8b2d940', '693ce69af3432bd00be13c3971a57961', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('1fe4d408b85f19618c15bcb768f0ec22', '1750a8fb3e6d90cb7957c02de1dc8e59', '9502685863ab87f0ad1134142788a385', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('2013784686234210306', 'f6817f48af4fb3af11b9e8bf182f618b', '1596335805278990338', NULL, '2026-01-21 09:24:18', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('2013784686234210307', 'f6817f48af4fb3af11b9e8bf182f618b', '1596141938193747970', NULL, '2026-01-21 09:24:18', '0:0:0:0:0:0:0:1');
INSERT INTO `sys_role_permission` VALUES ('248d288586c6ff3bd14381565df84163', '52b0cf022ac4187b2a70dfa4f8b2d940', '3f915b2769fc80648e92d04e84ca059d', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('27489816708b18859768dfed5945c405', 'a799c3b1b12dd3ed4bd046bfaef5fe6e', '9502685863ab87f0ad1134142788a385', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('296f9c75ca0e172ae5ce4c1022c996df', '646c628b2b8295fbdab2d34044de0354', '732d48f8e0abe99fe6a23d18a3171cd1', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('2c462293cbb0eab7e8ae0a3600361b5f', '52b0cf022ac4187b2a70dfa4f8b2d940', '9502685863ab87f0ad1134142788a385', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('2d943d8a073211f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_access_settings', NULL, '2026-02-11 18:12:31', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('2d953f8b073211f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_chat_window_settings', NULL, '2026-02-11 18:12:31', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('2d95b8e8073211f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_sensitive_words', NULL, '2026-02-11 18:12:31', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('2fdaed22dfa4c8d4629e44ef81688c6a', '52b0cf022ac4187b2a70dfa4f8b2d940', 'aedbf679b5773c1f25e9f7b10111da73', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('300c462b7fec09e2ff32574ef8b3f0bd', '52b0cf022ac4187b2a70dfa4f8b2d940', '2a470fc0c3954d9dbb61de6d80846549', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('35ac7cae648de39eb56213ca1b649713', '52b0cf022ac4187b2a70dfa4f8b2d940', 'b1cb0a3fedf7ed0e4653cb5a229837ee', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('3e563751942b0879c88ca4de19757b50', '1750a8fb3e6d90cb7957c02de1dc8e59', '58857ff846e61794c69208e9d3a85466', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('412e2de37a35b3442d68db8dd2f3c190', '52b0cf022ac4187b2a70dfa4f8b2d940', 'f1cb187abf927c88b89470d08615f5ac', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('4204f91fb61911ba8ce40afa7c02369f', 'f6817f48af4fb3af11b9e8bf182f618b', '3f915b2769fc80648e92d04e84ca059d', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('4faad8ff93cb2b5607cd3d07c1b624ee', 'a799c3b1b12dd3ed4bd046bfaef5fe6e', '70b8f33da5f39de1981bf89cf6c99792', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('57c0b3a547b815ea3ec8e509b08948b3', '1750a8fb3e6d90cb7957c02de1dc8e59', '3f915b2769fc80648e92d04e84ca059d', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('593ee05c4fe4645c7826b7d5e14f23ec', '52b0cf022ac4187b2a70dfa4f8b2d940', '8fb8172747a78756c11916216b8b8066', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('5fc194b709336d354640fe29fefd65a3', 'a799c3b1b12dd3ed4bd046bfaef5fe6e', '9ba60e626bf2882c31c488aba62b89f0', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('7102d8ceebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_parent', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('7102edf8ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_workbench', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('710343d4ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_agent', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('710344beebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_conversation', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('71034685ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_quick_reply', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('710347e9ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_agent_add', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('71034869ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_agent_edit', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('71034935ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_agent_delete', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('7103d1b6ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_conversation_view', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('7103d2d8ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_conversation_export', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('7103d600ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_quick_reply_add', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('7103d686ebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_quick_reply_edit', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('7103d6ebebaa11f0ac8d82cc2e90d14d', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_quick_reply_delete', NULL, '2026-01-07 17:22:52', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('726197a3075511f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_quick_access', NULL, '2026-02-11 22:24:59', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('726261b9075511f1bb1ed6be57400c00', 'cs_admin_agent_role_001', 'cs_quick_access', NULL, '2026-02-11 22:24:59', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('75002588591820806', '16457350655250432', '5129710648430592', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('75002588604403712', '16457350655250432', '5129710648430593', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('75002588612792320', '16457350655250432', '40238597734928384', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('75002588625375232', '16457350655250432', '57009744761589760', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('75002588633763840', '16457350655250432', '16392452747300864', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('75002588637958144', '16457350655250432', '16392767785668608', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('75002588650541056', '16457350655250432', '16439068543946752', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277779875336192', '496138616573952', '5129710648430592', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780043108352', '496138616573952', '5129710648430593', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780055691264', '496138616573952', '15701400130424832', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780064079872', '496138616573952', '16678126574637056', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780072468480', '496138616573952', '15701915807518720', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780076662784', '496138616573952', '15708892205944832', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780085051392', '496138616573952', '16678447719911424', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780089245696', '496138616573952', '25014528525733888', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780097634304', '496138616573952', '56898976661639168', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780135383040', '496138616573952', '40238597734928384', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780139577344', '496138616573952', '45235621697949696', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780147965952', '496138616573952', '45235787867885568', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780156354560', '496138616573952', '45235939278065664', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780164743168', '496138616573952', '43117268627886080', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780168937472', '496138616573952', '45236734832676864', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780181520384', '496138616573952', '45237010692050944', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780189908992', '496138616573952', '45237170029465600', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780198297600', '496138616573952', '57009544286441472', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780206686208', '496138616573952', '57009744761589760', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780215074816', '496138616573952', '57009981228060672', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780219269120', '496138616573952', '56309618086776832', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780227657728', '496138616573952', '57212882168844288', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780236046336', '496138616573952', '61560041605435392', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780244434944', '496138616573952', '61560275261722624', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780257017856', '496138616573952', '61560480518377472', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780265406464', '496138616573952', '44986029924421632', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780324126720', '496138616573952', '45235228800716800', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780332515328', '496138616573952', '45069342940860416', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780340903937', '496138616573952', '5129710648430594', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780349292544', '496138616573952', '16687383932047360', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780357681152', '496138616573952', '16689632049631232', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780366069760', '496138616573952', '16689745006432256', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780370264064', '496138616573952', '16689883993083904', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780374458369', '496138616573952', '16690313745666048', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780387041280', '496138616573952', '5129710648430595', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780395429888', '496138616573952', '16694861252005888', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780403818496', '496138616573952', '16695107491205120', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780412207104', '496138616573952', '16695243126607872', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780420595712', '496138616573952', '75002207560273920', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780428984320', '496138616573952', '76215889006956544', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780433178624', '496138616573952', '76216071333351424', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780441567232', '496138616573952', '76216264070008832', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780449955840', '496138616573952', '76216459709124608', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780458344448', '496138616573952', '76216594207870976', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780466733056', '496138616573952', '76216702639017984', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780475121664', '496138616573952', '58480609315524608', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780483510272', '496138616573952', '61394706252173312', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780491898880', '496138616573952', '61417744146370560', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780496093184', '496138616573952', '76606430504816640', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780504481792', '496138616573952', '76914082455752704', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780508676097', '496138616573952', '76607201262702592', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780517064704', '496138616573952', '39915540965232640', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780525453312', '496138616573952', '41370251991977984', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780538036224', '496138616573952', '45264987354042368', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780546424832', '496138616573952', '45265487029866496', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780554813440', '496138616573952', '45265762415284224', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780559007744', '496138616573952', '45265886315024384', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780567396352', '496138616573952', '45266070000373760', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780571590656', '496138616573952', '41363147411427328', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780579979264', '496138616573952', '41363537456533504', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780588367872', '496138616573952', '41364927394353152', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780596756480', '496138616573952', '41371711400054784', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780605145088', '496138616573952', '41469219249852416', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780613533696', '496138616573952', '39916171171991552', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780621922304', '496138616573952', '39918482854252544', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780630310912', '496138616573952', '41373430515240960', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780718391296', '496138616573952', '41375330996326400', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780722585600', '496138616573952', '63741744973352960', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780730974208', '496138616573952', '42082442672082944', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780739362816', '496138616573952', '41376192166629376', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780747751424', '496138616573952', '41377034236071936', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780756140032', '496138616573952', '56911328312299520', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780764528640', '496138616573952', '41378916912336896', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780768722944', '496138616573952', '63482475359244288', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780772917249', '496138616573952', '64290663792906240', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780785500160', '496138616573952', '66790433014943744', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780789694464', '496138616573952', '42087054753927168', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780798083072', '496138616573952', '67027338952445952', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780806471680', '496138616573952', '67027909637836800', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780810665985', '496138616573952', '67042515441684480', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780823248896', '496138616573952', '67082402312228864', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780827443200', '496138616573952', '16392452747300864', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780835831808', '496138616573952', '16392767785668608', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780840026112', '496138616573952', '16438800255291392', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780844220417', '496138616573952', '16438962738434048', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277780852609024', '496138616573952', '16439068543946752', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860062040064', '496138616573953', '5129710648430592', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860070428672', '496138616573953', '5129710648430593', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860078817280', '496138616573953', '40238597734928384', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860091400192', '496138616573953', '43117268627886080', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860099788800', '496138616573953', '57009744761589760', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860112371712', '496138616573953', '56309618086776832', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860120760320', '496138616573953', '44986029924421632', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860129148928', '496138616573953', '5129710648430594', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860141731840', '496138616573953', '5129710648430595', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860150120448', '496138616573953', '75002207560273920', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860158509056', '496138616573953', '58480609315524608', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860162703360', '496138616573953', '76606430504816640', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860171091968', '496138616573953', '76914082455752704', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860179480576', '496138616573953', '76607201262702592', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860187869184', '496138616573953', '39915540965232640', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860196257792', '496138616573953', '41370251991977984', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860204646400', '496138616573953', '41363147411427328', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860208840704', '496138616573953', '41371711400054784', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860213035009', '496138616573953', '39916171171991552', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860221423616', '496138616573953', '39918482854252544', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860225617920', '496138616573953', '41373430515240960', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860234006528', '496138616573953', '41375330996326400', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860242395136', '496138616573953', '63741744973352960', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860250783744', '496138616573953', '42082442672082944', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860254978048', '496138616573953', '41376192166629376', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860263366656', '496138616573953', '41377034236071936', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860271755264', '496138616573953', '56911328312299520', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860313698304', '496138616573953', '41378916912336896', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860322086912', '496138616573953', '63482475359244288', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860326281216', '496138616573953', '64290663792906240', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860334669824', '496138616573953', '66790433014943744', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860343058432', '496138616573953', '42087054753927168', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860347252736', '496138616573953', '67027338952445952', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860351447041', '496138616573953', '67027909637836800', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860359835648', '496138616573953', '67042515441684480', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860364029952', '496138616573953', '67082402312228864', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860368224256', '496138616573953', '16392452747300864', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860372418560', '496138616573953', '16392767785668608', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860376612865', '496138616573953', '16438800255291392', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860385001472', '496138616573953', '16438962738434048', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('77277860389195776', '496138616573953', '16439068543946752', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('7a5d31ba48fe3fb1266bf186dc5f7ba7', '52b0cf022ac4187b2a70dfa4f8b2d940', '58857ff846e61794c69208e9d3a85466', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('7de42bdc0b8c5446b7d428c66a7abc12', '52b0cf022ac4187b2a70dfa4f8b2d940', '54dd5457a3190740005c1bfec55b1c34', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('7e19d90cec0dd87aaef351b9ff8f4902', '646c628b2b8295fbdab2d34044de0354', 'f9d3f4f27653a71c52faa9fb8070fbe7', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('8506be87075511f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', '9502685863ab87f0ad1134142788a385', NULL, '2026-02-11 22:25:31', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('85072b82075511f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', '1438108176273760258', NULL, '2026-02-11 22:25:31', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('8507a36c075511f1bb1ed6be57400c00', 'cs_admin_agent_role_001', '9502685863ab87f0ad1134142788a385', NULL, '2026-02-11 22:25:31', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('85084138075511f1bb1ed6be57400c00', 'cs_admin_agent_role_001', '1438108176273760258', NULL, '2026-02-11 22:25:31', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('8a60df8d8b4c9ee5fa63f48aeee3ec00', '1750a8fb3e6d90cb7957c02de1dc8e59', 'd7d6e2e4e2934f2c9385a623fd98c6f3', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('8ce1022dac4e558ff9694600515cf510', '1750a8fb3e6d90cb7957c02de1dc8e59', '08e6b9dc3c04489c8e1ff2ce6f105aa4', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('8d848ca7feec5b7ebb3ecb32b2c8857a', '52b0cf022ac4187b2a70dfa4f8b2d940', '4148ec82b6acd69f470bea75fe41c357', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('9264104cee9b10c96241d527b2d0346d', '1750a8fb3e6d90cb7957c02de1dc8e59', '54dd5457a3190740005c1bfec55b1c34', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('980171fda43adfe24840959b1d048d4d', 'f6817f48af4fb3af11b9e8bf182f618b', 'd7d6e2e4e2934f2c9385a623fd98c6f3', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('9d8772c310b675ae43eacdbc6c7fa04a', 'a799c3b1b12dd3ed4bd046bfaef5fe6e', '1663f3faba244d16c94552f849627d84', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('9f8311ecccd44e079723098cf2ffe1cc', '1750a8fb3e6d90cb7957c02de1dc8e59', '693ce69af3432bd00be13c3971a57961', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('aefc8c22e061171806e59cd222f6b7e1', '52b0cf022ac4187b2a70dfa4f8b2d940', 'e8af452d8948ea49d37c934f5100ae6a', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_access_settings', 'cs_admin_agent_role_001', 'cs_access_settings', NULL, '2026-02-11 18:21:16', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_agent', 'cs_admin_agent_role_001', 'cs_agent', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_app', 'cs_admin_agent_role_001', '1893865471550578689', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_app_del', 'cs_admin_agent_role_001', '1930221335938662401', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_app_edit', 'cs_admin_agent_role_001', '1930221213607591937', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_chat', 'cs_admin_agent_role_001', '1895401981290643458', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_flow', 'cs_admin_agent_role_001', '1890213291321749505', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_flow_add', 'cs_admin_agent_role_001', '1930222862556266498', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_flow_dbg', 'cs_admin_agent_role_001', '1930223114757611522', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_flow_del', 'cs_admin_agent_role_001', '1930223132619112449', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_flow_edit', 'cs_admin_agent_role_001', '1930222953853681666', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_flow_save', 'cs_admin_agent_role_001', '1930223034757611522', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb', 'cs_admin_agent_role_001', '1892557342028226561', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_add', 'cs_admin_agent_role_001', '1930221570324758530', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_del', 'cs_admin_agent_role_001', '1930221774230847490', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_doc_bd', 'cs_admin_agent_role_001', '1930222295012409345', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_doc_da', 'cs_admin_agent_role_001', '1930222395180777474', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_doc_edit', 'cs_admin_agent_role_001', '1930221983555977217', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_doc_rb', 'cs_admin_agent_role_001', '1930222218734796802', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_doc_zip', 'cs_admin_agent_role_001', '1930222066120851457', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_edit', 'cs_admin_agent_role_001', '1930221637551063042', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_kb_rebuild', 'cs_admin_agent_role_001', '1930221702164316161', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_mcp', 'cs_admin_agent_role_001', '1980223355087781889', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_model', 'cs_admin_agent_role_001', '1892553778493022209', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_model_add', 'cs_admin_agent_role_001', '1930222558582472705', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_model_del', 'cs_admin_agent_role_001', '1930222679269376001', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_model_edit', 'cs_admin_agent_role_001', '1930222617197871105', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_ocr', 'cs_admin_agent_role_001', '1912753560201089025', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_airag_parent', 'cs_admin_agent_role_001', '1892553163993931777', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_auto_msg', 'cs_admin_agent_role_001', 'cs_auto_message', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_brand', 'cs_admin_agent_role_001', 'cs_brand_config', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_brand_save', 'cs_admin_agent_role_001', 'cs_brand_save', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_chat_win_settings', 'cs_admin_agent_role_001', 'cs_chat_window_settings', NULL, '2026-02-11 18:21:16', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_conversation', 'cs_admin_agent_role_001', 'cs_conversation', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_conv_assign', 'cs_admin_agent_role_001', 'cs_conversation_assign', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_mb_records', 'cs_admin_agent_role_001', 'cs_message_board_records', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_mb_settings', 'cs_admin_agent_role_001', 'cs_message_board_settings', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_parent', 'cs_admin_agent_role_001', 'cs_parent', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_quick_reply', 'cs_admin_agent_role_001', 'cs_quick_reply', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_sensitive_words', 'cs_admin_agent_role_001', 'cs_sensitive_words', NULL, '2026-02-11 18:21:16', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_sub_agent', 'cs_admin_agent_role_001', 'cs_sub_agent', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_sys_parent', 'cs_admin_agent_role_001', 'd7d6e2e4e2934f2c9385a623fd98c6f3', NULL, '2026-02-09 15:22:29', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_admin_perm_workbench', 'cs_admin_agent_role_001', 'cs_workbench', NULL, '2026-02-09 14:45:01', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_auto_msg_perm_01', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_auto_message', NULL, '2026-02-06 17:10:58', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_conv_assign_perm_01', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_conversation_assign', NULL, '2026-02-06 15:09:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_mb_records_perm_01', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_message_board_records', NULL, '2026-02-06 15:09:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_mb_settings_perm_01', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_message_board_settings', NULL, '2026-02-06 15:09:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_admin_ipbl', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_ip_blacklist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_admin_log', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_agent_login_log', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_admin_parent', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_security', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_admin_vbl', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_visitor_blacklist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_admin_wl', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_agent_ip_whitelist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_cadmin_ipbl', 'cs_admin_agent_role_001', 'cs_ip_blacklist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_cadmin_log', 'cs_admin_agent_role_001', 'cs_agent_login_log', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_cadmin_parent', 'cs_admin_agent_role_001', 'cs_security', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_cadmin_vbl', 'cs_admin_agent_role_001', 'cs_visitor_blacklist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_cadmin_wl', 'cs_admin_agent_role_001', 'cs_agent_ip_whitelist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_sub_ipbl', 'cs_sub_agent_role_001', 'cs_ip_blacklist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_sub_parent', 'cs_sub_agent_role_001', 'cs_security', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sec_sub_vbl', 'cs_sub_agent_role_001', 'cs_visitor_blacklist', NULL, '2026-02-09 16:44:00', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_agent_perm_01', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_sub_agent', NULL, '2026-02-09 11:42:38', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_auto_msg', 'cs_sub_agent_role_001', 'cs_auto_message', NULL, '2026-02-09 15:14:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_conversation', 'cs_sub_agent_role_001', 'cs_conversation', NULL, '2026-02-09 15:14:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_conv_assign', 'cs_sub_agent_role_001', 'cs_conversation_assign', NULL, '2026-02-09 15:14:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_mb_records', 'cs_sub_agent_role_001', 'cs_message_board_records', NULL, '2026-02-09 15:14:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_mb_settings', 'cs_sub_agent_role_001', 'cs_message_board_settings', NULL, '2026-02-09 15:14:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_parent', 'cs_sub_agent_role_001', 'cs_parent', NULL, '2026-02-09 11:42:38', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_quick_reply', 'cs_sub_agent_role_001', 'cs_quick_reply', NULL, '2026-02-09 15:14:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('cs_sub_perm_workbench', 'cs_sub_agent_role_001', 'cs_workbench', NULL, '2026-02-09 11:42:38', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('d3fe195d59811531c05d31d8436f5c8b', '1750a8fb3e6d90cb7957c02de1dc8e59', 'e8af452d8948ea49d37c934f5100ae6a', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('dd48e3fe073211f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_access_settings', NULL, '2026-02-11 18:17:26', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('dd4a3a28073211f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_chat_window_settings', NULL, '2026-02-11 18:17:26', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('dd4abe57073211f1bb1ed6be57400c00', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_sensitive_words', NULL, '2026-02-11 18:17:26', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('dea74b33f5ce11f089be56ca9804d785', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_brand_config', NULL, '2026-01-20 15:08:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('dea76150f5ce11f089be56ca9804d785', 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_brand_save', NULL, '2026-01-20 15:08:49', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('e3e922673f4289b18366bb51b6200f17', '52b0cf022ac4187b2a70dfa4f8b2d940', '45c966826eeff4c99b8f8ebfe74511fc', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('f17ab8ad1e71341140857ef4914ef297', '21c5a3187763729408b40afb0d0fdfa8', '732d48f8e0abe99fe6a23d18a3171cd1', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('fed41a4671285efb266cd404f24dd378', '52b0cf022ac4187b2a70dfa4f8b2d940', '00a2a0ae65cdca5e93209cdbde97cbe6', NULL, NULL, NULL);
INSERT INTO `sys_role_permission` VALUES ('sys_lic_perm_admin', 'f6817f48af4fb3af11b9e8bf182f618b', 'sys_license_status', NULL, '2026-02-25 15:59:09', '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES ('sys_lic_perm_cs_admin', 'cs_admin_agent_role_001', 'sys_license_status', NULL, '2026-02-25 15:59:09', '127.0.0.1');

-- ----------------------------
-- Table structure for sys_sms
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms`;
CREATE TABLE `sys_sms`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'ID',
  `es_title` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '消息标题',
  `es_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '发送方式：参考枚举MessageTypeEnum',
  `es_receiver` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '接收人',
  `es_param` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '发送所需参数Json格式',
  `es_content` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '推送内容',
  `es_send_time` datetime NULL DEFAULT NULL COMMENT '推送时间',
  `es_send_status` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '推送状态 0未推送 1推送成功 2推送失败 -1失败不再发送',
  `es_send_num` int NULL DEFAULT NULL COMMENT '发送次数 超过5次不再发送',
  `es_result` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '推送失败原因',
  `remark` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ss_es_type`(`es_type` ASC) USING BTREE,
  INDEX `idx_ss_es_receiver`(`es_receiver` ASC) USING BTREE,
  INDEX `idx_ss_es_send_time`(`es_send_time` ASC) USING BTREE,
  INDEX `idx_ss_es_send_status`(`es_send_status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_sms
-- ----------------------------
INSERT INTO `sys_sms` VALUES ('402880e74dc2f361014dc2f8411e0001', '消息推送测试333', '2', '411944058@qq.com', NULL, '张三你好，你的订单4028d881436d514601436d521ae80165已付款!', '2015-06-05 17:06:01', '3', NULL, NULL, '认证失败错误的用户名或者密码', 'admin', '2015-06-05 17:05:59', 'admin', '2015-11-19 22:30:39');
INSERT INTO `sys_sms` VALUES ('402880ea533647b00153364e74770001', '发个问候', '3', 'admin', NULL, '你好', '2016-03-02 00:00:00', '2', NULL, NULL, NULL, 'admin', '2016-03-02 15:50:24', 'admin', '2018-07-05 19:53:01');
INSERT INTO `sys_sms` VALUES ('402880ee5a17e711015a17f3188e013f', '消息推送测试333', '2', '411944058@qq.com', NULL, '张三你好，你的订单4028d881436d514601436d521ae80165已付款!', NULL, '2', NULL, NULL, NULL, 'admin', '2017-02-07 17:41:31', 'admin', '2017-03-10 11:37:05');
INSERT INTO `sys_sms` VALUES ('402880f05ab649b4015ab64b9cd80012', '消息推送测试333', '2', '411944058@qq.com', NULL, '张三你好，你的订单4028d881436d514601436d521ae80165已付款!', '2017-11-16 15:58:15', '3', NULL, NULL, NULL, 'admin', '2017-03-10 11:38:13', 'admin', '2017-07-31 17:24:54');
INSERT INTO `sys_sms` VALUES ('402880f05ab7b035015ab7c4462c0004', '消息推送测试333', '2', '411944058@qq.com', NULL, '张三你好，你的订单4028d881436d514601436d521ae80165已付款!', '2017-11-16 15:58:15', '3', NULL, NULL, NULL, 'admin', '2017-03-10 18:29:37', NULL, NULL);
INSERT INTO `sys_sms` VALUES ('402881f3646a472b01646a4a5af00001', '催办：HR审批', '3', 'admin', NULL, 'admin，您好！\r\n请前待办任务办理事项！HR审批\r\n\r\n\r\n===========================\r\n此消息由系统发出', '2018-07-05 19:53:35', '2', NULL, NULL, NULL, 'admin', '2018-07-05 19:53:35', 'admin', '2018-07-07 13:45:24');
INSERT INTO `sys_sms` VALUES ('402881f3647da06c01647da43a940014', '催办：HR审批', '3', 'admin', NULL, 'admin，您好！\r\n请前待办任务办理事项！HR审批\r\n\r\n\r\n===========================\r\n此消息由系统发出', '2018-07-09 14:04:32', '2', NULL, NULL, NULL, 'admin', '2018-07-09 14:04:32', 'admin', '2018-07-09 18:51:30');

-- ----------------------------
-- Table structure for sys_sms_template
-- ----------------------------
DROP TABLE IF EXISTS `sys_sms_template`;
CREATE TABLE `sys_sms_template`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键',
  `template_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '模板标题',
  `template_code` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '模板CODE',
  `template_type` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '模板类型：1短信 2邮件 3微信',
  `template_category` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '模版分类：notice通知公告 other其他',
  `template_content` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '模板内容',
  `template_test_json` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '模板测试json',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `use_status` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否使用中 1是0否',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sst_template_code`(`template_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_sms_template
-- ----------------------------
INSERT INTO `sys_sms_template` VALUES ('1199606397416775681', '系统消息通知', 'sys_ts_note', '2', NULL, '<h1>&nbsp; &nbsp; 系统通知</h1>\n<ul>\n<li>通知时间：&nbsp; ${ts_date}</li>\n<li>通知内容：&nbsp; ${ts_content}</li>\n</ul>', NULL, '2019-11-27 16:30:27', 'admin', '2019-11-27 19:36:50', 'admin', NULL);
INSERT INTO `sys_sms_template` VALUES ('1199615897335095298', '流程催办', 'bpm_cuiban', '2', NULL, '<h1>&nbsp; &nbsp;流程催办提醒</h1>\n<ul>\n<li>流程名称：&nbsp; ${bpm_name}</li>\n<li>催办任务：&nbsp; ${bpm_task}</li>\n<li>催办时间 :&nbsp; &nbsp; ${datetime}</li>\n<li>催办内容 :&nbsp; &nbsp; ${remark}</li>\n</ul>', NULL, '2019-11-27 17:08:12', 'admin', '2019-11-27 19:36:45', 'admin', NULL);
INSERT INTO `sys_sms_template` VALUES ('1199648914107625473', '流程办理超时提醒', 'bpm_chaoshi_tip', '2', NULL, '<h1>&nbsp; &nbsp;流程办理超时提醒</h1>\n<ul>\n<li>&nbsp; &nbsp;超时提醒信息：&nbsp; &nbsp; 您有待处理的超时任务，请尽快处理！</li>\n<li>&nbsp; &nbsp;超时任务标题：&nbsp; &nbsp; ${title}</li>\n<li>&nbsp; &nbsp;超时任务节点：&nbsp; &nbsp; ${task}</li>\n<li>&nbsp; &nbsp;任务处理人：&nbsp; &nbsp; &nbsp; &nbsp;${user}</li>\n<li>&nbsp; &nbsp;任务开始时间：&nbsp; &nbsp; ${time}</li>\n</ul>', NULL, '2019-11-27 19:19:24', 'admin', '2019-11-27 19:36:37', 'admin', NULL);
INSERT INTO `sys_sms_template` VALUES ('4028608164691b000164693108140003', '催办：${taskName}', 'SYS001', '1', NULL, '${userName}，您好！\r\n请前待办任务办理事项！${taskName}\r\n\r\n\r\n===========================\r\n此消息由系统发出', '{\r\n\"taskName\":\"HR审批\",\r\n\"userName\":\"admin\"\r\n}', '2018-07-05 14:46:18', 'admin', '2018-07-05 18:31:34', 'admin', NULL);

-- ----------------------------
-- Table structure for sys_table_white_list
-- ----------------------------
DROP TABLE IF EXISTS `sys_table_white_list`;
CREATE TABLE `sys_table_white_list`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `table_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '允许的表名',
  `field_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '允许的字段名，多个用逗号分割',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '1' COMMENT '状态，1=启用，0=禁用',
  `create_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_sys_table_white_list_table_name`(`table_name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统表白名单' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_table_white_list
-- ----------------------------
INSERT INTO `sys_table_white_list` VALUES ('1701578033271521282', 'sys_user', 'phone,work_no,id,email,realname,username', '1', 'admin', '2023-09-12 10:46:32', 'admin', '2023-12-31 16:55:30');
INSERT INTO `sys_table_white_list` VALUES ('1701581935488385025', 'oa_officialdoc_organcode', 'id,organ_name', '1', 'admin', '2023-09-12 11:02:02', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1701581977733414913', 'demo', 'id,name', '1', 'admin', '2023-09-12 11:02:12', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1701582035472203777', 'sys_permission', 'id,name', '1', 'admin', '2023-09-12 11:02:26', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1701582087619985409', 'onl_drag_comp', 'id,comp_name', '1', 'admin', '2023-09-12 11:02:38', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1701582136420712450', 'sys_depart', 'id,org_code,depart_name', '1', 'admin', '2023-09-12 11:02:50', 'admin', '2023-10-18 09:36:40');
INSERT INTO `sys_table_white_list` VALUES ('1701582163599802370', 'design_form', 'id,desform_name,desform_code', '1', 'admin', '2023-09-12 11:02:56', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1701582190187495426', 'onl_cgform_head', 'table_txt,table_name', '1', 'admin', '2023-09-12 11:03:03', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1701582254301626370', 'oa_wps_file', 'id,name', '1', 'admin', '2023-09-12 11:03:18', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1714453996678926338', 'onl_cgreport_head', 'code', '1', 'admin', '2023-10-18 09:31:00', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1714455418728337410', 'sys_category', 'id,name', '1', 'admin', '2023-10-18 09:36:40', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1714471625900564482', 'sys_position', 'name,id', '1', 'ceshi', '2023-10-18 10:41:04', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1769610154632491009', 'sys_dict', 'dict_code', '1', 'admin', '2024-03-18 14:21:53', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1778692300030484482', 'test_shoptype_tree', 'type_name,id', '1', 'admin', '2024-04-12 15:51:05', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1782650226206269441', 'sys_tenant', 'name,id', '1', 'admin', '2024-04-23 13:58:29', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1800712552062898178', 'tj_user_report', 'name,username', '1', 'admin', '2024-06-12 10:11:43', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1801076145102925826', 'sys_data_source', 'code,name', '1', 'admin', '2024-06-13 10:16:30', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1801097090085564420', 'sys_role', 'role_name,role_code', '1', 'jeecg', '2024-06-13 11:39:44', 'admin', '2024-09-10 11:47:35');
INSERT INTO `sys_table_white_list` VALUES ('1805416360756006913', 'wu_liao', 'wul_name,id', '1', 'admin', '2024-06-25 09:42:58', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1897919397122269185', 'ces_shop_type', 'name,pid,id,has_child', '1', 'admin', '2025-03-07 15:57:01', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1907407400953659394', 'airag_flow', 'name,id', '1', 'admin', '2025-04-02 20:18:57', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1907407401083682817', 'airag_model', 'name,id', '1', 'admin', '2025-04-02 20:18:57', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1950438522834546690', 'sys_sms_template', 'template_code', '1', 'admin', '2025-07-30 14:09:16', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1966817706103730178', 'sys_check_rule', 'rule_code', '1', 'admin', '2025-09-13 18:54:17', NULL, NULL);
INSERT INTO `sys_table_white_list` VALUES ('1993972426358153217', 'airag_knowledge', 'name,id', '1', 'admin', '2025-11-27 17:17:27', NULL, NULL);

-- ----------------------------
-- Table structure for sys_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant`  (
  `id` int NOT NULL COMMENT '租户编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '租户名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `begin_date` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_date` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `status` int NULL DEFAULT NULL COMMENT '状态 1正常 0冻结',
  `trade` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '所属行业',
  `company_size` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公司规模',
  `company_address` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公司地址',
  `company_logo` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公司logo',
  `house_number` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '门牌号',
  `work_place` varchar(100) CHARACTER SET utf32 COLLATE utf32_general_ci NULL DEFAULT NULL COMMENT '工作地点',
  `secondary_domain` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '二级域名',
  `login_bkgd_img` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录背景图片',
  `position` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职级',
  `department` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门',
  `del_flag` tinyint(1) NULL DEFAULT 0 COMMENT '删除状态(0-正常,1-已删除)',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `apply_status` int NULL DEFAULT NULL COMMENT '允许申请管理员 1允许 0不允许',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '多租户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant
-- ----------------------------
INSERT INTO `sys_tenant` VALUES (1000, '北京国炬信息技术有限公司', '2023-03-09 19:55:11', 'jeecg', NULL, NULL, 1, NULL, NULL, NULL, 'temp/国炬软件logo_1764069407429.png', '2PI3U6', NULL, NULL, NULL, NULL, NULL, 0, 'admin', '2025-11-25 19:16:47', NULL);
INSERT INTO `sys_tenant` VALUES (1001, '北京敲敲云科技有限公司', '2023-10-18 13:37:19', 'ceshi', NULL, NULL, 1, NULL, NULL, NULL, '', 'EX33W8', NULL, NULL, NULL, NULL, NULL, 0, 'admin', '2024-03-18 11:19:28', NULL);

-- ----------------------------
-- Table structure for sys_tenant_pack
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_pack`;
CREATE TABLE `sys_tenant_pack`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `tenant_id` int NULL DEFAULT NULL COMMENT '租户id',
  `pack_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '产品包名',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '开启状态(0 未开启 1开启)',
  `remarks` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` date NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` date NULL DEFAULT NULL COMMENT '更新时间',
  `pack_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '编码,默认添加的三个管理员需要设置编码',
  `pack_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'custom' COMMENT '产品包类型(default 默认产品包 custom 自定义产品包)',
  `iz_sysn` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '自动分配给用户(0否 1是)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx__stp_tenant_id_pack_code`(`tenant_id` ASC, `pack_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户产品包' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant_pack
-- ----------------------------
INSERT INTO `sys_tenant_pack` VALUES ('1714517098074152962', 0, '默认套餐', '1', NULL, 'ceshi', '2023-10-18', 'admin', '2025-08-12', NULL, 'default', NULL);
INSERT INTO `sys_tenant_pack` VALUES ('1955184115322142722', 1000, '默认套餐', '1', NULL, 'ceshi', '2023-10-18', 'admin', '2025-08-12', NULL, 'custom', NULL);
INSERT INTO `sys_tenant_pack` VALUES ('1955187901394534401', 1000, '升级企业套餐', '1', NULL, 'admin', '2025-08-12', 'admin', '2025-08-12', NULL, 'custom', NULL);
INSERT INTO `sys_tenant_pack` VALUES ('1955222289125720066', 1001, '默认套餐', '1', NULL, 'ceshi', '2023-10-18', 'admin', '2025-08-12', NULL, 'custom', NULL);

-- ----------------------------
-- Table structure for sys_tenant_pack_perms
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_pack_perms`;
CREATE TABLE `sys_tenant_pack_perms`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键编号',
  `pack_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '租户产品包名称',
  `permission_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单id',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` date NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` date NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stpp_pack_id`(`pack_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户产品包和菜单关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant_pack_perms
-- ----------------------------
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955183422662197249', '1714517098074152962', '1438108225451974658', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955184115389251585', '1955184115322142722', '1438108225451974658', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955187901482614785', '1955187901394534401', '1438108176273760258', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955187901503586305', '1955187901394534401', '1438108176814825473', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955187901516169218', '1955187901394534401', '1620709334357532673', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955187901528752129', '1955187901394534401', '9502685863ab87f0ad1134142788a385', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195388533358593', '1955187901394534401', '1438108187455774722', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195388600467458', '1955187901394534401', '1438108178911977473', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195388659187713', '1955187901394534401', '1438108183395688450', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195509216067585', '1955187901394534401', '1439398677984878593', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195509216067586', '1955187901394534401', '1438108225451974658', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195623284359170', '1955187901394534401', '1443390062919208961', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195663855861761', '1955187901394534401', '1438108183492157442', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955195717383569409', '1955187901394534401', '119213522910765570', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955221560482840577', '1714517098074152962', '1438108176273760258', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955221560617058305', '1714517098074152962', '9502685863ab87f0ad1134142788a385', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955221560617058306', '1714517098074152962', '1620709334357532673', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955221560617058307', '1714517098074152962', '1438108176814825473', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222179079766017', '1714517098074152962', '1438108187455774722', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222179146874881', '1714517098074152962', '1438108178911977473', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222289125720067', '1955222289125720066', '1438108225451974658', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222289125720068', '1955222289125720066', '1438108176273760258', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222289125720069', '1955222289125720066', '9502685863ab87f0ad1134142788a385', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222289125720070', '1955222289125720066', '1620709334357532673', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222289125720071', '1955222289125720066', '1438108176814825473', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222289188634625', '1955222289125720066', '1438108187455774722', 'admin', '2025-08-12', NULL, NULL);
INSERT INTO `sys_tenant_pack_perms` VALUES ('1955222289188634626', '1955222289125720066', '1438108178911977473', 'admin', '2025-08-12', NULL, NULL);

-- ----------------------------
-- Table structure for sys_tenant_pack_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_pack_user`;
CREATE TABLE `sys_tenant_pack_user`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `pack_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '租户产品包ID',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户ID',
  `tenant_id` int NULL DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `status` int NULL DEFAULT NULL COMMENT '状态 正常状态1 申请状态0',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_tpu_pack_id`(`pack_id` ASC) USING BTREE,
  INDEX `idx_tpu_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_tpu_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_tpu_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户套餐人员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_tenant_pack_user
-- ----------------------------
INSERT INTO `sys_tenant_pack_user` VALUES ('1633795234318729217', '1633795213938606082', 'a75d45a015c44384a04449ee80dc3503', 1, 'admin', '2023-03-09 19:41:53', NULL, NULL, 1);
INSERT INTO `sys_tenant_pack_user` VALUES ('1955184602037567490', '1955184115322142722', '1955183658394664962', 1000, 'admin', '2025-08-12 16:28:29', NULL, NULL, 1);
INSERT INTO `sys_tenant_pack_user` VALUES ('1955187972634787841', '1955187901394534401', '1955183658394664962', 1000, 'admin', '2025-08-12 16:41:53', NULL, NULL, 1);
INSERT INTO `sys_tenant_pack_user` VALUES ('1955222312760623107', '1955222289125720066', '1955218082645544962', 1001, 'admin', '2025-08-12 18:58:20', NULL, NULL, 1);

-- ----------------------------
-- Table structure for sys_third_account
-- ----------------------------
DROP TABLE IF EXISTS `sys_third_account`;
CREATE TABLE `sys_third_account`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '编号',
  `sys_user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方登录id',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '状态(1-正常,2-冻结)',
  `del_flag` tinyint(1) NULL DEFAULT NULL COMMENT '删除状态(0-正常,1-已删除)',
  `realname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户id',
  `third_user_uuid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方账号',
  `third_user_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方app用户账号',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `third_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '登录来源',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_stat_third_type_user_id`(`third_type` ASC, `third_user_id` ASC) USING BTREE,
  UNIQUE INDEX `uniq_sta_third_user_id_third_type`(`third_user_id` ASC, `third_type` ASC, `tenant_id` ASC) USING BTREE,
  UNIQUE INDEX `uniq_sta_third_user_uuid_third_type`(`third_user_uuid` ASC, `third_type` ASC, `tenant_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_third_account
-- ----------------------------

-- ----------------------------
-- Table structure for sys_third_app_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_third_app_config`;
CREATE TABLE `sys_third_app_config`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `tenant_id` int NOT NULL DEFAULT 0 COMMENT '租户id',
  `agent_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '钉钉/企业微信应用id',
  `client_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '钉钉/企业微信 应用id',
  `client_secret` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '钉钉/企业微信应用id对应的秘钥',
  `corp_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '钉钉企业id',
  `third_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '第三方类别(dingtalk 钉钉 wechat_enterprise 企业微信)',
  `status` int NULL DEFAULT 1 COMMENT '是否启用(0-否,1-是)',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_stac_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_stac_third_type`(`third_type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '租户第三方配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_third_app_config
-- ----------------------------
INSERT INTO `sys_third_app_config` VALUES ('1714477134884085762', 0, '1', '1', '1', NULL, 'dingtalk', 1, '2023-10-18 11:02:57', NULL);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `username` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '登录账号',
  `realname` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `password` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密码',
  `salt` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'md5密码盐',
  `avatar` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '头像',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `sex` tinyint(1) NULL DEFAULT NULL COMMENT '性别(0-默认未知,1-男,2-女)',
  `email` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '电子邮件',
  `phone` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '电话',
  `org_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '登录会话的机构编码',
  `status` tinyint(1) NULL DEFAULT NULL COMMENT '性别(1-正常,2-冻结)',
  `del_flag` tinyint(1) NULL DEFAULT NULL COMMENT '删除状态(0-正常,1-已删除)',
  `third_id` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '第三方登录的唯一标识',
  `third_type` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '第三方类型',
  `activiti_sync` tinyint(1) NULL DEFAULT NULL COMMENT '同步工作流引擎(1-同步,0-不同步)',
  `work_no` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '工号，唯一键',
  `telephone` varchar(45) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '座机号',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `user_identity` tinyint(1) NULL DEFAULT NULL COMMENT '身份（1普通成员 2上级）',
  `depart_ids` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '负责部门',
  `client_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '设备ID',
  `login_tenant_id` int NULL DEFAULT NULL COMMENT '上次登录选择租户ID',
  `bpm_status` varchar(2) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '流程入职离职状态',
  `sign_enable` tinyint(1) NULL DEFAULT NULL COMMENT '是否启用个性签名（0 否 1是）',
  `sign` varchar(500) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '个性签名',
  `main_dep_post_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主岗位（部门岗位id）',
  `position_type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '职务(字典)',
  `last_pwd_update_time` datetime NULL DEFAULT NULL COMMENT '上一次修改密码的时间',
  `sort` int NULL DEFAULT NULL COMMENT '排序',
  `iz_hide_contact` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否隐藏联系方式（0 否 1是）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_sys_user_work_no`(`work_no` ASC) USING BTREE,
  UNIQUE INDEX `uniq_sys_user_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uniq_sys_user_phone`(`phone` ASC) USING BTREE,
  UNIQUE INDEX `uniq_sys_user_email`(`email` ASC) USING BTREE,
  INDEX `idx_su_status`(`status` ASC) USING BTREE,
  INDEX `idx_su_del_flag`(`del_flag` ASC) USING BTREE,
  INDEX `idx_su_del_username`(`username` ASC, `del_flag` ASC) USING BTREE,
  INDEX `idx_su_main_dep_post_id`(`main_dep_post_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES ('e9ca23d68d884d4ebb19d07889727dae', 'admin', '管理员', 'cb362cfeefbf3d8d', 'RCGTeGiH', '', '1986-02-01', 1, 'jeecg@163.com', '18611111111', NULL, 1, 0, NULL, NULL, 1, '00001', NULL, NULL, '2019-06-21 17:54:10', 'admin', '2026-02-09 17:15:30', 2, '', NULL, 1000, NULL, 0, NULL, '', NULL, '2025-11-25 15:42:34', 1000, NULL);

-- ----------------------------
-- Table structure for sys_user_dep_post
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_dep_post`;
CREATE TABLE `sys_user_dep_post`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `dep_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门岗位id',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sudp_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_sudp_dep_id`(`dep_id` ASC) USING BTREE,
  INDEX `idx_sudp_user_dep_id`(`user_id` ASC, `dep_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_dep_post
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user_depart
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_depart`;
CREATE TABLE `sys_user_depart`  (
  `ID` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'id',
  `user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `dep_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '部门id',
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE INDEX `idx_sud_user_dep_id`(`user_id` ASC, `dep_id` ASC) USING BTREE,
  INDEX `idx_sud_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_sud_dep_id`(`dep_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_depart
-- ----------------------------
INSERT INTO `sys_user_depart` VALUES ('1783729121915510785', '3d464b4ea0d2491aab8a7bde74c57e95', 'a7d7e77e06c84325a40932163adcdaa6');
INSERT INTO `sys_user_depart` VALUES ('1714519577067200514', 'a75d45a015c44384a04449ee80dc3503', 'a7d7e77e06c84325a40932163adcdaa6');
INSERT INTO `sys_user_depart` VALUES ('1f3a0267811327b9eca86b0cc2b956f3', 'bcbe1290783a469a83ae3bd8effe15d4', '5159cde220114246b045e574adceafe9');

-- ----------------------------
-- Table structure for sys_user_position
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_position`;
CREATE TABLE `sys_user_position`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `position_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '职位id',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sup_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_sup_position_id`(`position_id` ASC) USING BTREE,
  INDEX `idx_sup_user_position_id`(`user_id` ASC, `position_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_position
-- ----------------------------
INSERT INTO `sys_user_position` VALUES ('1920390985293144065', '1714471285016895490', '1185040064792571906', 'ceshi', '2025-05-08 16:11:05', NULL, NULL);

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键id',
  `user_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `role_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '角色id',
  `tenant_id` int NULL DEFAULT 0 COMMENT '租户ID',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sur_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_sur_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_sur_user_role_id`(`user_id` ASC, `role_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '用户角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES ('2013804772537962497', '2011347033756512258', 'cs_admin_agent_role_001', 0);
INSERT INTO `sys_user_role` VALUES ('2013804798072885249', '2011347178300616706', 'cs_admin_agent_role_001', 0);
INSERT INTO `sys_user_role` VALUES ('2019667482325991426', '2019667482284048386', 'f6817f48af4fb3af11b9e8bf182f618b', 0);
INSERT INTO `sys_user_role` VALUES ('2020788636927361026', 'e9ca23d68d884d4ebb19d07889727dae', 'f6817f48af4fb3af11b9e8bf182f618b', 0);
INSERT INTO `sys_user_role` VALUES ('37465915f80945518057ad08f04f7ba3', '2ff9a2dc9a224906b92a41ec83c61ba5', 'cs_admin_agent_role_001', 0);
INSERT INTO `sys_user_role` VALUES ('996fffcd84e3430b82aff2120a7a950c', '46025e06ba294332934eeae4304d3a4e', 'cs_sub_agent_role_001', 0);
INSERT INTO `sys_user_role` VALUES ('b3ffd9311a1ca296c44e2409b547384f', '01b802058ea94b978a2c96f4807f6b48', '1', 0);
INSERT INTO `sys_user_role` VALUES ('ee45d0343ecec894b6886effc92cb0b7', '4d8fef4667574b24a9ccfedaf257810c', 'f6817f48af4fb3af11b9e8bf182f618b', 0);
INSERT INTO `sys_user_role` VALUES ('f2922a38ba24fb53749e45a0c459adb3', '439ae3e9bcf7418583fcd429cadb1d72', '1', 0);

-- ----------------------------
-- Table structure for sys_user_tenant
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_tenant`;
CREATE TABLE `sys_user_tenant`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `user_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户id',
  `tenant_id` int NULL DEFAULT NULL COMMENT '租户id',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态(1 正常 2 离职 3 待审核 4 拒绝 5 邀请加入)',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sut_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_sut_tenant_id`(`tenant_id` ASC) USING BTREE,
  INDEX `idx_sut_user_rel_tenant`(`user_id` ASC, `tenant_id` ASC) USING BTREE,
  INDEX `idx_sut_status`(`status` ASC) USING BTREE,
  INDEX `idx_sut_userid_status`(`user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户租户关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user_tenant
-- ----------------------------
INSERT INTO `sys_user_tenant` VALUES ('1955179797651038210', 'a75d45a015c44384a04449ee80dc3503', 1001, '1', 'admin', '2025-08-12 16:09:24', NULL, NULL);
INSERT INTO `sys_user_tenant` VALUES ('1955182032762126337', 'e9ca23d68d884d4ebb19d07889727dae', 1000, '1', 'admin', '2025-08-12 16:18:17', NULL, NULL);
INSERT INTO `sys_user_tenant` VALUES ('1955211766602534913', '1714471285016895490', 1001, '1', 'admin', '2025-08-12 18:16:26', NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
