package de.exxcellent.microservices.showcase.core.country.impl.business;

import de.exxcellent.microservices.showcase.common.errorhandling.ErrorCode;
import de.exxcellent.microservices.showcase.common.errorhandling.exception.BusinessException;
import de.exxcellent.microservices.showcase.common.validation.Preconditions;
import de.exxcellent.microservices.showcase.core.country.impl.access.CountryValidation;
import de.exxcellent.microservices.showcase.core.country.impl.persistence.CountryRepository;
import de.exxcellent.microservices.showcase.core.country.impl.persistence.model.CountryET;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;

/**
 * Manages countries. Implementation of {@link CountryICI}.
 *
 * @author Felix Riess
 * @since 20.01.20
 */
@ApplicationScoped
public class CountryManager implements CountryICI {
    /**
     * The {@link Logger} for this {@link CountryManager}.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CountryManager.class);

    private final CountryRepository countryRepository;

    @Inject
    CountryManager(final CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Override
    public Set<CountryET> getCountries() {
        LOG.info("Query storage to get all available countries");
        return this.countryRepository.findAll();
    }

    @Override
    public CountryET getCountry(final String shortName) {
        Preconditions.checkNotNull(shortName, "Country short name must not be null");
        Preconditions.checkStringLength(shortName, 3, "Country short name must have 3 characters");
        LOG.info("Query storage for country with short name {}", shortName);
        final Optional<CountryET> optionalCountry = this.countryRepository.findByShortName(shortName);
        if(optionalCountry.isPresent()) {
            return optionalCountry.get();
        } else {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Country with short name " + shortName + " is not existing");
        }
    }

    @Override
    public Set<CountryET> addCountry(final CountryET country) {
        CountryValidation.validateCountryET(country);
        LOG.info("Query storage for country with short name {} to avoid generating duplicates", country.getShortName());
        final Optional<CountryET> optionalCountryET = this.countryRepository.findByShortName(country.getShortName());
        if(optionalCountryET.isPresent()) {
            final CountryET existingCountry = optionalCountryET.get();
            LOG.info("Country with short name {} is already existing in storage: {}", existingCountry.getShortName(), existingCountry.getName());
            if(!existingCountry.getName().equalsIgnoreCase(country.getName())) {
                // another country with this short name is already existing. No more country with this short name can be created.
                throw new BusinessException(ErrorCode.INVALID_ARGUMENT_ERROR, "A country with the short name "
                        + country.getShortName() + " is already existing: " + existingCountry.getName()
                        + ". Cannot create two countries with the same short name");
            } // else: country is already existing and must not be added again.
        } else {
            LOG.info("Adding new country {} with short name {} to storage", country.getName(), country.getShortName());
            // no country with the provided short name is present. Create a new one.
            this.countryRepository.addCountry(country);
        }
        return this.countryRepository.findAll();
    }
}
