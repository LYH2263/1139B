package com.wordmind.service;

import com.wordmind.dto.TagDTO;
import com.wordmind.entity.Tag;
import com.wordmind.entity.Word;
import com.wordmind.entity.WordTag;
import com.wordmind.repository.TagRepository;
import com.wordmind.repository.WordRepository;
import com.wordmind.repository.WordTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TagService {

    private static final int MAX_TAGS_PER_USER = 20;

    private static final Set<String> PRESET_COLORS = new HashSet<>(Arrays.asList(
            "#409EFF", "#67C23A", "#E6A23C", "#F56C6C",
            "#909399", "#8E44AD", "#16A085", "#D35400"
    ));

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private WordTagRepository wordTagRepository;

    @Autowired
    private WordRepository wordRepository;

    public List<TagDTO.Response> getTagsByUserId(Long userId) {
        List<Tag> tags = tagRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return tags.stream()
                .map(tag -> convertToDTO(tag, userId))
                .collect(Collectors.toList());
    }

    public TagDTO.Response getTagById(Long userId, Long tagId) {
        Tag tag = tagRepository.findByUserIdAndId(userId, tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        return convertToDTO(tag, userId);
    }

    @Transactional
    public TagDTO.Response createTag(Long userId, TagDTO.CreateRequest request) {
        String trimmedName = request.getName().trim();
        if (trimmedName.isEmpty()) {
            throw new RuntimeException("标签名不能为空");
        }

        validateColor(request.getColor());

        long count = tagRepository.countByUserId(userId);
        if (count >= MAX_TAGS_PER_USER) {
            throw new RuntimeException("每个用户最多只能创建 " + MAX_TAGS_PER_USER + " 个标签");
        }

        if (tagRepository.existsByUserIdAndName(userId, trimmedName)) {
            throw new RuntimeException("标签名已存在");
        }

        Tag tag = new Tag();
        tag.setUserId(userId);
        tag.setName(trimmedName);
        tag.setColor(request.getColor());
        Tag saved = tagRepository.save(tag);

        return convertToDTO(saved, userId);
    }

    @Transactional
    public TagDTO.Response updateTag(Long userId, Long tagId, TagDTO.UpdateRequest request) {
        Tag tag = tagRepository.findByUserIdAndId(userId, tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new RuntimeException("标签名不能为空");
            }
            if (!trimmedName.equals(tag.getName()) && tagRepository.existsByUserIdAndName(userId, trimmedName)) {
                throw new RuntimeException("标签名已存在");
            }
            tag.setName(trimmedName);
        }

        if (request.getColor() != null) {
            validateColor(request.getColor());
            tag.setColor(request.getColor());
        }

        Tag saved = tagRepository.save(tag);
        return convertToDTO(saved, userId);
    }

    @Transactional
    public void deleteTag(Long userId, Long tagId) {
        Tag tag = tagRepository.findByUserIdAndId(userId, tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));
        wordTagRepository.deleteByUserIdAndTagId(userId, tagId);
        tagRepository.delete(tag);
    }

    public List<TagDTO.Response> getWordTags(Long userId, Long wordId) {
        wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        List<Long> tagIds = wordTagRepository.findTagIdsByUserIdAndWordId(userId, wordId);
        List<Tag> tags = tagRepository.findAllById(tagIds);
        return tags.stream()
                .map(tag -> convertToDTO(tag, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public TagDTO.Response bindTagToWord(Long userId, Long wordId, Long tagId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        Tag tag = tagRepository.findByUserIdAndId(userId, tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        if (wordTagRepository.existsByUserIdAndWordIdAndTagId(userId, wordId, tagId)) {
            throw new RuntimeException("该标签已绑定到此单词");
        }

        WordTag wordTag = new WordTag();
        wordTag.setUserId(userId);
        wordTag.setWordId(wordId);
        wordTag.setTagId(tagId);
        wordTagRepository.save(wordTag);

        return convertToDTO(tag, userId);
    }

    @Transactional
    public void unbindTagFromWord(Long userId, Long wordId, Long tagId) {
        wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));

        tagRepository.findByUserIdAndId(userId, tagId)
                .orElseThrow(() -> new RuntimeException("标签不存在"));

        if (!wordTagRepository.existsByUserIdAndWordIdAndTagId(userId, wordId, tagId)) {
            throw new RuntimeException("该标签未绑定到此单词");
        }

        wordTagRepository.deleteByUserIdAndWordIdAndTagId(userId, wordId, tagId);
    }

    public List<Long> getWordIdsByTags(Long userId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return null;
        }
        List<Long> wordIds = wordTagRepository.findWordIdsByUserIdAndTagIds(userId, tagIds);
        if (tagIds.size() <= 1) {
            return wordIds;
        }
        return wordIds.stream()
                .distinct()
                .filter(wordId -> {
                    List<Long> boundTagIds = wordTagRepository.findTagIdsByUserIdAndWordId(userId, wordId);
                    return new HashSet<>(boundTagIds).containsAll(tagIds);
                })
                .collect(Collectors.toList());
    }

    private void validateColor(String color) {
        if (!PRESET_COLORS.contains(color)) {
            throw new RuntimeException("无效的标签颜色");
        }
    }

    private TagDTO.Response convertToDTO(Tag tag, Long userId) {
        int wordCount = wordTagRepository.findWordIdsByUserIdAndTagId(userId, tag.getId()).size();
        return TagDTO.Response.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .createdAt(tag.getCreatedAt())
                .wordCount(wordCount)
                .build();
    }
}
