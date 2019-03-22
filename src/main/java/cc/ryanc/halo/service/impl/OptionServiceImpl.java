package cc.ryanc.halo.service.impl;

import cc.ryanc.halo.model.dto.OptionOutputDTO;
import cc.ryanc.halo.model.entity.Option;
import cc.ryanc.halo.model.enums.BlogProperties;
import cc.ryanc.halo.model.params.OptionParam;
import cc.ryanc.halo.repository.OptionRepository;
import cc.ryanc.halo.service.OptionService;
import cc.ryanc.halo.service.base.AbstractCrudService;
import cc.ryanc.halo.utils.ServiceUtils;
import com.qiniu.common.Zone;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OptionService implementation class
 *
 * @author : RYAN0UP
 * @date : 2019-03-14
 */
@Slf4j
@Service
public class OptionServiceImpl extends AbstractCrudService<Option, Integer> implements OptionService {

    private final OptionRepository optionRepository;

    public OptionServiceImpl(OptionRepository optionRepository) {
        super(optionRepository);
        this.optionRepository = optionRepository;
    }

    /**
     * Saves one option
     *
     * @param key    key
     * @param value  value
     * @param source source
     */
    @Override
    public void save(String key, String value, String source) {
        Assert.hasText(key, "Option key must not be blank");

        if (StringUtils.isNotBlank(value)) {
            // If the value is blank, remove the key
            optionRepository.removeByOptionKey(key);
            return;
        }

        Option option = optionRepository.findByOptionKey(key).map(anOption -> {
            // Exist
            anOption.setOptionValue(value);
            return anOption;
        }).orElseGet(() -> {
            // Not exist
            Option anOption = new Option();
            anOption.setOptionKey(key);
            anOption.setOptionValue(value);
            anOption.setOptionSource(source);
            return anOption;
        });

        // Save or update the options
        optionRepository.save(option);
    }

    /**
     * Saves multiple options
     *
     * @param options options
     * @param source  source
     */
    @Override
    public void save(Map<String, String> options, String source) {
        if (CollectionUtils.isEmpty(options)) {
            return;
        }

        // (Not recommended) Don't write "this::save" here
        // Types of key and value are String
        options.forEach((key, value) -> save(key, value, source));
    }

    @Override
    public void save(List<OptionParam> optionParams, String source) {
        if (CollectionUtils.isEmpty(optionParams)) {
            return;
        }

        // TODO Optimize the query
        optionParams.forEach(optionParam -> save(optionParam.getOptionKey(), optionParam.getOptionValue(), source));
    }

    @Override
    public void saveProperties(Map<BlogProperties, String> properties, String source) {
        if (CollectionUtils.isEmpty(properties)) {
            return;
        }

        properties.forEach((property, value) -> save(property.getValue(), value, source));
    }

    /**
     * Gets all options
     *
     * @return Map
     */
    @Override
    public Map<String, String> listOptions() {
        return ServiceUtils.convertToMap(listAll(), Option::getOptionKey, Option::getOptionValue);
    }

    @Override
    public List<OptionOutputDTO> listDtos() {
        return listAll().stream().map(option -> (OptionOutputDTO) new OptionOutputDTO().convertFrom(option)).collect(Collectors.toList());
    }

    /**
     * Gets option by key
     *
     * @param key key
     * @return String
     */
    @Override
    public String getByKeyOfNullable(String key) {
        return getByKey(key).orElse(null);
    }

    @Override
    public Optional<String> getByKey(String key) {
        Assert.hasText(key, "Option key must not be blank");

        return optionRepository.findByOptionKey(key).map(Option::getOptionValue);
    }

    @Override
    public String getByPropertyOfNullable(BlogProperties property) {
        return getByProperty(property).orElse(null);
    }

    @Override
    public Optional<String> getByProperty(BlogProperties property) {
        Assert.notNull(property, "Blog property must not be null");

        return getByKey(property.getValue());
    }

    @Override
    public int getPostPageSize() {
        try {
            return getByProperty(BlogProperties.INDEX_POSTS).map(Integer::valueOf).orElse(DEFAULT_POST_PAGE_SIZE);
        } catch (NumberFormatException e) {
            log.error(BlogProperties.INDEX_POSTS + " option was not a number format", e);
            return DEFAULT_POST_PAGE_SIZE;
        }
    }

    @Override
    public int getCommentPageSize() {
        try {
            return getByProperty(BlogProperties.INDEX_COMMENTS).map(Integer::valueOf).orElse(DEFAULT_COMMENT_PAGE_SIZE);
        } catch (NumberFormatException e) {
            log.error(BlogProperties.INDEX_COMMENTS + " option was not a number format", e);
            return DEFAULT_COMMENT_PAGE_SIZE;
        }
    }

    @Override
    public Zone getQiniuZone() {
        return getByProperty(BlogProperties.QINIU_ZONE).map(qiniuZone -> {

            Zone zone;
            switch (qiniuZone) {
                case "z0":
                    zone = Zone.zone0();
                    break;
                case "z1":
                    zone = Zone.zone1();
                    break;
                case "z2":
                    zone = Zone.zone2();
                    break;
                case "na0":
                    zone = Zone.zoneNa0();
                    break;
                case "as0":
                    zone = Zone.zoneAs0();
                    break;
                default:
                    // Default is detecting zone automatically
                    zone = Zone.autoZone();
            }
            return zone;

        }).orElseGet(Zone::autoZone);
    }


}