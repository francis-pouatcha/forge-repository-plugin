package org.adorsys.forge.plugins.repo;

import javax.persistence.Entity;
import java.io.Serializable;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Version;
import java.lang.Override;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class CompanyJPA implements Serializable
{

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   @Column(name = "id", updatable = false, nullable = false)
   private Long id = null;
   @Version
   @Column(name = "version")
   private int version = 0;

   @Column
   @NotNull(message = "companyJpa_company_name_null")
   @Size(message = "companyJpa_company_name_size", min = 2)
   private String name;

   @Column
   @NotNull(message = "companyJpa_company_street_null")
   @Size(message = "companyJpa_company_street_size", min = 3)
   private String street;

   @Column
   @NotNull(message = "companyJpa_company_zip_null")
   @Size(message = "companyJpa_company_zip_size", min = 3)
   private String zip;

   @Column
   @NotNull(message = "companyJpa_company_country_null")
   @Size(message = "companyJpa_company_country_size", min = 3)
   private String country;

   @Column
   @NotNull(message = "companyJpa_company_city_null")
   @Size(message = "companyJpa_company_city_size", min = 3)
   private String city;

   @Column
   @NotNull(message = "companyJpa_company_email_null")
   @Size(message = "companyJpa_company_email_size", min = 6)
   private String email;

   @Column
   @NotNull(message = "companyJpa_company_fax_null")
   @Size(message = "companyJpa_company_fax_size", min = 6)
   private String fax;

   @Column
   @NotNull(message = "companyJpa_company_phone_null")
   @Size(message = "companyJpa_company_phone_size", min = 6)
   private String phone;

   @Column
   @Size(message = "companyJpa_company_id_null", min = 3, max = 3)
   @NotNull(message = "companyJpa_company_id_size")
   private String companyId;

   @Column
   @NotNull(message = "companyJpa_company_adviderName_null")
   @Size(message = "companyJpa_company_adviderName_size", min = 2)
   private String adviserName;

   @Temporal(TemporalType.TIMESTAMP)
   private Date lastlyModified;

   @Temporal(TemporalType.TIMESTAMP)
   private Date created;

   public Long getId()
   {
      return this.id;
   }

   public void setId(final Long id)
   {
      this.id = id;
   }

   public int getVersion()
   {
      return this.version;
   }

   public void setVersion(final int version)
   {
      this.version = version;
   }

   @Override
   public boolean equals(Object that)
   {
      if (this == that)
      {
         return true;
      }
      if (that == null)
      {
         return false;
      }
      if (getClass() != that.getClass())
      {
         return false;
      }
      if (id != null)
      {
         return id.equals(((CompanyJPA) that).id);
      }
      return super.equals(that);
   }

   @Override
   public int hashCode()
   {
      if (id != null)
      {
         return id.hashCode();
      }
      return super.hashCode();
   }

   public String getName()
   {
      return this.name;
   }

   public void setName(final String name)
   {
      this.name = name;
   }

   public String getStreet()
   {
      return this.street;
   }

   public void setStreet(final String street)
   {
      this.street = street;
   }

   public String getZip()
   {
      return this.zip;
   }

   public void setZip(final String zip)
   {
      this.zip = zip;
   }

   public String getCountry()
   {
      return this.country;
   }

   public void setCountry(final String country)
   {
      this.country = country;
   }

   public String getCity()
   {
      return this.city;
   }

   public void setCity(final String city)
   {
      this.city = city;
   }

   public String getEmail()
   {
      return this.email;
   }

   public void setEmail(final String email)
   {
      this.email = email;
   }

   public String getFax()
   {
      return this.fax;
   }

   public void setFax(final String fax)
   {
      this.fax = fax;
   }

   public String getPhone()
   {
      return this.phone;
   }

   public void setPhone(final String phone)
   {
      this.phone = phone;
   }

   public String getCompanyId()
   {
      return this.companyId;
   }

   public void setCompanyId(final String companyId)
   {
      this.companyId = companyId;
   }

   public String getAdviserName()
   {
      return this.adviserName;
   }

   public void setAdviserName(final String adviserName)
   {
      this.adviserName = adviserName;
   }

   public Date getLastlyModified()
   {
      return this.lastlyModified;
   }

   public void setLastlyModified(final Date lastlyModified)
   {
      this.lastlyModified = lastlyModified;
   }

   public Date getCreated()
   {
      return this.created;
   }

   public void setCreated(final Date created)
   {
      this.created = created;
   }

   @Override
   public String toString()
   {
      String result = getClass().getSimpleName() + " ";
      if (name != null && !name.trim().isEmpty())
         result += "name: " + name;
      if (street != null && !street.trim().isEmpty())
         result += ", street: " + street;
      if (zip != null && !zip.trim().isEmpty())
         result += ", zip: " + zip;
      if (country != null && !country.trim().isEmpty())
         result += ", country: " + country;
      if (city != null && !city.trim().isEmpty())
         result += ", city: " + city;
      if (email != null && !email.trim().isEmpty())
         result += ", email: " + email;
      if (fax != null && !fax.trim().isEmpty())
         result += ", fax: " + fax;
      if (phone != null && !phone.trim().isEmpty())
         result += ", phone: " + phone;
      if (companyId != null && !companyId.trim().isEmpty())
         result += ", companyId: " + companyId;
      if (adviserName != null && !adviserName.trim().isEmpty())
         result += ", adviserName: " + adviserName;
      return result;
   }
}