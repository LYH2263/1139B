package com.wordmind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordmind.dto.AssociationDTO;
import com.wordmind.entity.MemoryAssociation;
import com.wordmind.entity.User;
import com.wordmind.entity.Word;
import com.wordmind.repository.MemoryAssociationRepository;
import com.wordmind.repository.UserRepository;
import com.wordmind.repository.WordRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssociationService {

    private static final String TYPE_ROOT = "词根拆解";
    private static final String TYPE_HOMOPHONE = "谐音联想";
    private static final String TYPE_LETTER = "字母联想";
    private static final String TYPE_USER = "用户分享";

    private List<PrefixEntry> prefixDict = new ArrayList<>();
    private List<SuffixEntry> suffixDict = new ArrayList<>();

    @Autowired
    private MemoryAssociationRepository associationRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Data
    public static class PrefixEntry {
        private String prefix;
        private String meaning;
        private String example;
    }

    @Data
    public static class SuffixEntry {
        private String suffix;
        private String meaning;
        private String example;
    }

    @PostConstruct
    public void init() throws IOException {
        loadPrefixDict();
        loadSuffixDict();
    }

    private void loadPrefixDict() throws IOException {
        ClassPathResource resource = new ClassPathResource("prefix_dict.json");
        JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
        JsonNode prefixesNode = rootNode.get("prefixes");
        prefixDict = objectMapper.readValue(
                objectMapper.treeAsTokens(prefixesNode),
                new TypeReference<List<PrefixEntry>>() {}
        );
        prefixDict.sort((a, b) -> b.getPrefix().length() - a.getPrefix().length());
    }

    private void loadSuffixDict() throws IOException {
        ClassPathResource resource = new ClassPathResource("suffix_dict.json");
        JsonNode rootNode = objectMapper.readTree(resource.getInputStream());
        JsonNode suffixesNode = rootNode.get("suffixes");
        suffixDict = objectMapper.readValue(
                objectMapper.treeAsTokens(suffixesNode),
                new TypeReference<List<SuffixEntry>>() {}
        );
        suffixDict.sort((a, b) -> b.getSuffix().length() - a.getSuffix().length());
    }

    @Transactional(readOnly = true)
    public AssociationDTO.ListResponse getAssociations(Long wordId, Long userId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        generateSystemAssociations(wordId, word.getWord(), word.getMeaning());

        Sort sort = Sort.by(
                Sort.Order.desc("upvotes"),
                Sort.Order.desc("createdAt")
        );
        List<MemoryAssociation> associations = associationRepository.findByWordId(wordId, sort);

        List<AssociationDTO.Response> list = associations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return AssociationDTO.ListResponse.builder()
                .list(list)
                .total((long) list.size())
                .build();
    }

    @Transactional
    public void generateSystemAssociations(Long wordId, String word, String meaning) {
        String lowerWord = word.toLowerCase();

        generateRootAssociation(wordId, lowerWord, meaning);
        generateHomophoneAssociation(wordId, lowerWord, meaning);
        generateLetterAssociation(wordId, lowerWord, meaning);
    }

    private void generateRootAssociation(Long wordId, String word, String meaning) {
        List<String> parts = new ArrayList<>();
        List<String> explanations = new ArrayList<>();
        String remaining = word;

        for (PrefixEntry entry : prefixDict) {
            if (remaining.startsWith(entry.getPrefix()) && remaining.length() > entry.getPrefix().length()) {
                parts.add(entry.getPrefix());
                explanations.add(entry.getPrefix() + "(" + entry.getMeaning() + ")");
                remaining = remaining.substring(entry.getPrefix().length());
                break;
            }
        }

        String stem = remaining;
        for (SuffixEntry entry : suffixDict) {
            if (stem.endsWith(entry.getSuffix()) && stem.length() > entry.getSuffix().length()) {
                String rootPart = stem.substring(0, stem.length() - entry.getSuffix().length());
                if (!rootPart.isEmpty()) {
                    parts.add(rootPart);
                    explanations.add(rootPart + "(词根)");
                }
                parts.add(entry.getSuffix());
                explanations.add(entry.getSuffix() + "(" + entry.getMeaning() + ")");
                remaining = "";
                break;
            }
        }

        if (!remaining.isEmpty() && !parts.contains(remaining)) {
            parts.add(remaining);
            explanations.add(remaining + "(词根)");
        }

        if (parts.size() >= 2) {
            String content = String.format("%s = %s%n释义：%s",
                    word,
                    String.join(" + ", explanations),
                    meaning);
            saveSystemAssociationIfNotExists(wordId, TYPE_ROOT, content);
        }
    }

    private void generateHomophoneAssociation(Long wordId, String word, String meaning) {
        Map<String, String> homophoneMap = new HashMap<>();
        homophoneMap.put("ambulance", "俺不能死 → 救护车来了，快救我！");
        homophoneMap.put("pest", "拍死它 → 害虫来了，快拍死！");
        homophoneMap.put("dilemma", "地雷吗？→ 进退两难啊！");
        homophoneMap.put("morbid", "毛病的 → 病态的");
        homophoneMap.put("fade", "废的 → 褪色了，废了");
        homophoneMap.put("obesity", "我必瘦 → 肥胖是我的敌人，我必瘦！");
        homophoneMap.put("agony", "爱过你 → 爱过你，失去你真痛苦");
        homophoneMap.put("mild", "温和的 → 温柔的");
        homophoneMap.put("parade", "怕累的 → 游行队伍里怕累的人都偷懒");
        homophoneMap.put("heroine", "好柔音 → 女主角的声音好柔美");
        homophoneMap.put("sentimental", "三屉馒头 → 多愁善感的人最爱吃三屉馒头");
        homophoneMap.put("economy", "依靠农民 → 经济发展要依靠农民");
        homophoneMap.put("bachelor", "白吃了 → 单身汉一个人白吃了");
        homophoneMap.put("crucial", "可入手 → 关键的时候可入手");
        homophoneMap.put("mercy", "摩西 → 摩西大发慈悲");
        homophoneMap.put("sympathy", "谁陪你 → 谁陪你，谁就同情你");
        homophoneMap.put("tremble", "船舶 → 船在颤抖");
        homophoneMap.put("appall", "我怕 → 我害怕，吓坏了");
        homophoneMap.put("horror", "好热 → 好热好恐怖");
        homophoneMap.put("famine", "发米呢 → 闹饥荒了，发米呢");
        homophoneMap.put("blush", "不拉屎 → 不拉屎憋得脸都红了");
        homophoneMap.put("coffin", "靠坟 → 靠坟的是棺材");
        homophoneMap.put("curse", "克死 → 诅咒克死你");
        homophoneMap.put("jail", "就哦 → 进监狱了，就哦一声");
        homophoneMap.put("luxurious", "卢克索瑞士 → 豪华旅行去卢克索和瑞士");
        homophoneMap.put("manage", "蛮记 → 蛮记下来就能管理好");
        homophoneMap.put("mask", "面罩 → 口罩、面罩");
        homophoneMap.put("monster", "怪物死他 → 怪物，死他！");
        homophoneMap.put("panic", "怕你 → 恐慌是因为怕你");
        homophoneMap.put("potato", "破土豆 → 土豆破了");
        homophoneMap.put("pretend", "葡萄藤 → 假装是葡萄藤");
        homophoneMap.put("vacation", "我开心 → 度假我开心");
        homophoneMap.put("volume", "我有肉 → 我有肉，体积大");
        homophoneMap.put("vanish", "我你消失 → 我和你都消失了");
        homophoneMap.put("vicious", "我射死 → 恶毒的人我射死");
        homophoneMap.put("bullet", "不理它 → 子弹飞来，不理它？不行！");
        homophoneMap.put("curtain", "卡通 → 窗帘上印着卡通");
        homophoneMap.put("feeble", "飞镖 → 飞镖是 feeble 无力的");
        homophoneMap.put("glare", "眩光 → 怒目而视");
        homophoneMap.put("grief", "贵妇 → 贵妇死了，真悲伤");
        homophoneMap.put("hatred", "恨得 → 仇恨恨得牙痒痒");
        homophoneMap.put("hostility", "好事体力 → 敌意要耗费好体力");
        homophoneMap.put("humble", "汉伯 → 谦卑的汉堡王");
        homophoneMap.put("idiot", "一点的 → 笨蛋一点的都不会");
        homophoneMap.put("inferno", "阴府楼 → 阴府楼就是地狱");
        homophoneMap.put("mansion", "蛮神 → 豪宅里住着蛮神");
        homophoneMap.put("marshal", "马首 → 元帅是马首是瞻");
        homophoneMap.put("mature", "成熟 → 成熟了");
        homophoneMap.put("meddle", "没动 → 别干涉，没动你");
        homophoneMap.put("mess", "马屎 → 一团糟像马屎");
        homophoneMap.put("miser", "吝啬 → 吝啬鬼");

        String homophone = homophoneMap.get(word);
        if (homophone != null) {
            String content = String.format("谐音联想：%s%n记忆：%s", word, homophone);
            saveSystemAssociationIfNotExists(wordId, TYPE_HOMOPHONE, content);
        } else if (word.length() >= 3) {
            String simpleHomophone = generateSimpleHomophone(word);
            if (simpleHomophone != null) {
                String content = String.format("谐音联想：%s ≈ %s%n记忆：%s",
                        word, simpleHomophone, meaning);
                saveSystemAssociationIfNotExists(wordId, TYPE_HOMOPHONE, content);
            }
        }
    }

    private String generateSimpleHomophone(String word) {
        Map<Character, String> pinyinMap = new HashMap<>();
        pinyinMap.put('a', "啊");
        pinyinMap.put('b', "不");
        pinyinMap.put('c', "看");
        pinyinMap.put('d', "的");
        pinyinMap.put('e', "一");
        pinyinMap.put('f', "发");
        pinyinMap.put('g', "哥");
        pinyinMap.put('h', "好");
        pinyinMap.put('i', "爱");
        pinyinMap.put('j', "记");
        pinyinMap.put('k', "可");
        pinyinMap.put('l', "了");
        pinyinMap.put('m', "吗");
        pinyinMap.put('n', "你");
        pinyinMap.put('o', "哦");
        pinyinMap.put('p', "朋");
        pinyinMap.put('q', "去");
        pinyinMap.put('r', "日");
        pinyinMap.put('s', "是");
        pinyinMap.put('t', "他");
        pinyinMap.put('u', "有");
        pinyinMap.put('v', "为");
        pinyinMap.put('w', "我");
        pinyinMap.put('x', "想");
        pinyinMap.put('y', "也");
        pinyinMap.put('z', "在");

        if (word.length() >= 3 && word.length() <= 6) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(word.length(), 4); i++) {
                char c = word.charAt(i);
                String py = pinyinMap.get(c);
                if (py != null) {
                    sb.append(py);
                }
            }
            if (sb.length() >= 2) {
                return sb.toString();
            }
        }
        return null;
    }

    private void generateLetterAssociation(Long wordId, String word, String meaning) {
        String content = null;

        if (word.equals("bed")) {
            content = "字母联想：bed → b和d像床的两根柱子，中间的e像一个人躺在床上睡觉 → 床";
        } else if (word.equals("eye")) {
            content = "字母联想：eye → e和e像两只眼睛，中间的y像鼻子 → 眼睛";
        } else if (word.equals("banana")) {
            content = "字母联想：banana → 一串香蕉，把a一个一个吃掉，最后剩下bn → 香蕉";
        } else if (word.equals("zoo")) {
            content = "字母联想：zoo → z像一只长颈鹿，两个o像动物园里的两只小熊猫 → 动物园";
        } else if (word.equals("book")) {
            content = "字母联想：book → 两个o像书的两页，b和k像书的封面和封底 → 书";
        } else if (word.equals("tree")) {
            content = "字母联想：tree → t像树干，r像树枝，两个e像树上的叶子 → 树";
        } else if (word.equals("moon")) {
            content = "字母联想：moon → m像远处的山，oo像天上的月亮，n像树下的人在赏月 → 月亮";
        } else if (word.equals("ball")) {
            content = "字母联想：ball → b像手，a像托起的动作，ll像球滚过的轨迹 → 球";
        } else if (word.equals("door")) {
            content = "字母联想：door → d像门把手，oo像门上的两个玻璃窗，r像门的支架 → 门";
        } else if (word.equals("fish")) {
            content = "字母联想：fish → f像鱼的尾巴，i像鱼身，s像鱼在水中游，h像吐出的泡泡 → 鱼";
        } else if (word.equals("cup")) {
            content = "字母联想：cup → c像杯子的把手，u像杯子的杯身，p像杯子的底座 → 杯子";
        } else if (word.equals("hat")) {
            content = "字母联想：hat → h像帽子的顶部，a像帽子的装饰，t像人的头戴着帽子 → 帽子";
        } else if (word.equals("sun")) {
            content = "字母联想：sun → s像太阳的光芒，u像太阳的轮廓，n像阳光照射下来 → 太阳";
        } else if (word.equals("star")) {
            content = "字母联想：star → s像星星闪烁，t像十字星，a像星星的尖角，r像星光 → 星星";
        } else if (word.equals("rain")) {
            content = "字母联想：rain → r像雨滴，a像伞，i像雨丝，n像人撑伞在雨中走 → 雨";
        } else if (word.equals("bird")) {
            content = "字母联想：bird → b像小鸟的翅膀，i像小鸟的身体，r像尾巴，d像小鸟的脚 → 鸟";
        } else if (word.equals("cake")) {
            content = "字母联想：cake → c像蛋糕的边缘，a像蜡烛，k像蛋糕上的水果，e像盘子 → 蛋糕";
        } else if (word.equals("hand")) {
            content = "字母联想：hand → h像手的大拇指，a像手掌，n像手指，d像手腕 → 手";
        } else if (word.equals("foot")) {
            content = "字母联想：foot → f像脚的形状，oo像脚趾，t像脚后跟 → 脚";
        } else if (word.equals("chair")) {
            content = "字母联想：chair → c像椅子的靠背，h像椅子的扶手，a像座板，i像椅子腿，r像椅子的支架 → 椅子";
        } else if (word.length() >= 3) {
            String letterAssoc = generatePatternBasedAssociation(word, meaning);
            if (letterAssoc != null) {
                content = letterAssoc;
            }
        }

        if (content != null) {
            saveSystemAssociationIfNotExists(wordId, TYPE_LETTER, content);
        }
    }

    private String generatePatternBasedAssociation(String word, String meaning) {
        char firstChar = word.charAt(0);
        char lastChar = word.charAt(word.length() - 1);

        if (word.length() == 3) {
            char middleChar = word.charAt(1);
            return String.format("字母联想：%s%n首字母「%c」像 %s 的头部%n中间「%c」像 %s 的身体%n尾字母「%c」像 %s 的尾部%n整体：%s",
                    word, firstChar, meaning, middleChar, meaning, lastChar, meaning, meaning);
        }

        if (word.length() == 4) {
            return String.format("字母联想：%s%n「%c」代表起点%n「%c」和「%c」代表过程%n「%c」代表终点%n想象一幅画面：%s",
                    word, firstChar, word.charAt(1), word.charAt(2), lastChar, meaning);
        }

        if (word.length() >= 5) {
            return String.format("字母联想：%s%n首字母「%c」开头定基调%n中间 %d 个字母构成主体%n尾字母「%c」收尾%n按顺序拼写，想象 %s 的画面",
                    word, firstChar, word.length() - 2, lastChar, meaning);
        }

        return null;
    }

    private void saveSystemAssociationIfNotExists(Long wordId, String type, String content) {
        if (!associationRepository.existsByWordIdAndTypeAndContentAndIsSystemGeneratedTrue(
                wordId, type, content)) {
            MemoryAssociation association = new MemoryAssociation();
            association.setWordId(wordId);
            association.setType(type);
            association.setContent(content);
            association.setUpvotes(10);
            association.setCreatedBy("SYSTEM");
            association.setIsSystemGenerated(true);
            associationRepository.save(association);
        }
    }

    @Transactional
    public AssociationDTO.Response createAssociation(Long userId, AssociationDTO.CreateRequest request) {
        Word word = wordRepository.findById(request.getWordId())
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        MemoryAssociation association = new MemoryAssociation();
        association.setWordId(request.getWordId());
        association.setType(request.getType());
        association.setContent(request.getContent());
        association.setUpvotes(0);
        association.setCreatedBy(user.getUsername());
        association.setIsSystemGenerated(false);

        MemoryAssociation saved = associationRepository.save(association);
        return convertToDTO(saved);
    }

    @Transactional
    public AssociationDTO.UpvoteResponse upvote(Long id) {
        MemoryAssociation association = associationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("联想不存在"));

        association.setUpvotes(association.getUpvotes() + 1);
        MemoryAssociation saved = associationRepository.save(association);

        return AssociationDTO.UpvoteResponse.builder()
                .id(saved.getId())
                .upvotes(saved.getUpvotes())
                .build();
    }

    private AssociationDTO.Response convertToDTO(MemoryAssociation association) {
        return AssociationDTO.Response.builder()
                .id(association.getId())
                .wordId(association.getWordId())
                .type(association.getType())
                .content(association.getContent())
                .upvotes(association.getUpvotes())
                .createdBy(association.getCreatedBy())
                .isSystemGenerated(association.getIsSystemGenerated())
                .createdAt(association.getCreatedAt())
                .build();
    }
}
