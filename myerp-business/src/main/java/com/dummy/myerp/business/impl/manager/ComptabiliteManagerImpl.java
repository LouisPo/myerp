package com.dummy.myerp.business.impl.manager;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.TransactionStatus;

import com.dummy.myerp.business.contrat.manager.ComptabiliteManager;
import com.dummy.myerp.business.impl.AbstractBusinessManager;
import com.dummy.myerp.model.bean.comptabilite.CompteComptable;
import com.dummy.myerp.model.bean.comptabilite.EcritureComptable;
import com.dummy.myerp.model.bean.comptabilite.JournalComptable;
import com.dummy.myerp.model.bean.comptabilite.LigneEcritureComptable;
import com.dummy.myerp.model.bean.comptabilite.SequenceEcritureComptable;
import com.dummy.myerp.technical.exception.FunctionalException;
import com.dummy.myerp.technical.exception.NotFoundException;
import com.dummy.myerp.technical.exception.TechnicalException;

/**
 * Comptabilite manager implementation.
 */
public class ComptabiliteManagerImpl extends AbstractBusinessManager implements ComptabiliteManager {

	// ==================== Attributs ====================
	/**
	 * the max reference number size
	 */
	private final static String REFERENCE_NUMBER_SIZE = "5";
	private SequenceEcritureComptable vSequenceEcritureComptable;

	// ==================== Constructeurs ====================
	/**
	 * Instantiates a new Comptabilite manager.
	 */
	public ComptabiliteManagerImpl() {
	}

	// ==================== Getters/Setters ====================

	@Override
	public List<CompteComptable> getListCompteComptable() {
		return getDaoProxy().getComptabiliteDao().getListCompteComptable();
	}

	// TODO added to allow mockito testing
	public SequenceEcritureComptable getvSequenceEcritureComptable() {
		return vSequenceEcritureComptable;
	}

	// TODO added to allow mockito testing
	public void setvSequenceEcritureComptable(SequenceEcritureComptable vSequenceEcritureComptable) {
		this.vSequenceEcritureComptable = vSequenceEcritureComptable;
	}

	// TODO added to allow mockito testing
	public void setvSequenceEcritureComptable(String pJournalCode, Integer pAnnee) throws NotFoundException {
		this.vSequenceEcritureComptable = getSequenceEcritureComptable(pJournalCode, pAnnee);
	}

	@Override
	public List<JournalComptable> getListJournalComptable() {
		List<JournalComptable> list = getDaoProxy().getComptabiliteDao().getListJournalComptable();
		return list;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<EcritureComptable> getListEcritureComptable() {
		return getDaoProxy().getComptabiliteDao().getListEcritureComptable();
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws NotFoundException if SequenceEcritureComptable not found note expected behaviour
	 * @throws FunctionalException if Reference not respect RG_Compta_0
	 */
	// FIXME ?? tester done
	// TODO add to report
	@Override
	public synchronized void addReference(EcritureComptable pEcritureComptable)
			throws NotFoundException, FunctionalException {
		// FIXME ?? impl??menter done delet this comment
		checkJournalComptable(pEcritureComptable, getListJournalComptable());
		try {
			this.setvSequenceEcritureComptable(pEcritureComptable.getJournal().getCode(), getYear(pEcritureComptable));
			this.setvSequenceEcritureComptable(
					this.addTheNewLastValueToSequenceEcritureComptable(getvSequenceEcritureComptable()));
			this.updateSequenceEcritureComptable(this.getvSequenceEcritureComptable());
			this.setEcritureComptableReference(pEcritureComptable, this.getvSequenceEcritureComptable());
		} catch (NotFoundException e) {
			this.setvSequenceEcritureComptable(this.createSequenceEcritureComptable(pEcritureComptable, 1));
			this.insertSequenceEcritureComptable(this.getvSequenceEcritureComptable());
			this.setEcritureComptableReference(pEcritureComptable, this.getvSequenceEcritureComptable());
		}

	}

	// FIXME delete this comment
	// Bien se r??ferer ?? la JavaDoc de cette m??thode !
	/*
	 * Le principe : 1. Remonter depuis la persitance la derni??re valeur de la
	 * s??quence du journal pour l'ann??e de l'??criture (table
	 * sequence_ecriture_comptable) 2. * S'il n'y a aucun enregistrement pour le
	 * journal pour l'ann??e concern??e : 1. Utiliser le num??ro 1. Sinon : 1. Utiliser
	 * la derni??re valeur + 1 3. Mettre ?? jour la r??f??rence de l'??criture avec la
	 * r??f??rence calcul??e (RG_Compta_5) 4. Enregistrer (insert/update) la valeur de
	 * la s??quence en persitance (table sequence_ecriture_comptable)
	 */

	/**
	 *
	 * @param pSequenceEcritureComptable the SequenceEcritureComptable to update
	 * @return pSequenceValueComptable updated with dernierValue +1
	 */
	protected SequenceEcritureComptable addTheNewLastValueToSequenceEcritureComptable(
			SequenceEcritureComptable pSequenceEcritureComptable) {
		pSequenceEcritureComptable.setDerniereValeur(pSequenceEcritureComptable.getDerniereValeur() + 1);
		return pSequenceEcritureComptable;
	}

	/**
	 *
	 * @param pEcritureComptable        EcritureComptable containing useFull
	 *                                  information
	 * @param pNewSequenceStartingValue The starting value
	 * @return new SequenceEcritureValue fill with information from
	 *         pEcritureComptable and dernierValue = pNewSequenceStartingValue
	 */
	protected SequenceEcritureComptable createSequenceEcritureComptable(EcritureComptable pEcritureComptable,
			Integer pNewSequenceStartingValue) {
		SequenceEcritureComptable vSequenceEcritureComptable = new SequenceEcritureComptable();
		vSequenceEcritureComptable = new SequenceEcritureComptable(LocalDate.now().getYear(), pNewSequenceStartingValue,
				pEcritureComptable.getJournal());
		return vSequenceEcritureComptable;
	}

	/**
	 *
	 * @param pEcritureComptable
	 * @throws FunctionalException FunctionalException if reference number length > 5
	 */

	/**
	 *
	 * @param pEcritureComptable the EcritureComptable where to set the reference
	 * @param pSequenceEcritureComptable The SequenceEcritureComptable used to set the reference
	 * @throws FunctionalException FunctionalException if reference number length &gt; 5
	 */
	protected void setEcritureComptableReference(EcritureComptable pEcritureComptable,
			SequenceEcritureComptable pSequenceEcritureComptable) throws FunctionalException {
		pEcritureComptable.setReference(
				createEcritureComptableReference(pEcritureComptable, pSequenceEcritureComptable.getDerniereValeur()));
	}

	/**
	 *
	 * @param pEcritureComptable used to set the reference
	 * @param pReferenceNumberValue the reference number to append
	 * @return the reference as string with regex [A-Z]{1,5}-\d{4}/\d{5}
	 * @throws FunctionalException FunctionalException if reference number length &gt; 5
	 */
	protected String createEcritureComptableReference(EcritureComptable pEcritureComptable,
			Integer pReferenceNumberValue) throws FunctionalException {
		StringBuilder vStrB = new StringBuilder();
		vStrB.append(pEcritureComptable.getJournal().getCode()).append("-").append(getYear(pEcritureComptable))
				.append("/").append(referenceNumberFormat(REFERENCE_NUMBER_SIZE, pReferenceNumberValue));
		return vStrB.toString();
	}
	/**
	 *
	 * @param pStringLenght max lenght of reference
	 * @param pInteger the Integer to parse as formated String
	 * @return the reference with 5 digits max completed with 0
	 * @throws FunctionalException if reference number length &gt; 5
	 */
	protected String referenceNumberFormat(String pStringLenght, Integer pInteger) throws FunctionalException {
		String vReferenceNumber = String.format("%0" + pStringLenght + "d", pInteger);
		if (vReferenceNumber.length() > 5) {
			throw new FunctionalException(" Le num??ro de r??f??rence: [" + vReferenceNumber + "] et hors limites");
		}
		return vReferenceNumber;
	}


	// FIXME ?? tester DONE


	@Override
	public void checkEcritureComptable(EcritureComptable pEcritureComptable)
			throws FunctionalException, NotFoundException {
		this.checkEcritureComptableUnit(pEcritureComptable);
		this.checkEcritureComptableContext(pEcritureComptable);
		this.checkIsJournalAndCompteComptableExist(pEcritureComptable);
	}

	/**
	 * V??rifie que l'Ecriture comptable respecte les r??gles de gestion unitaires,
	 * c'est ?? dire ind??pendemment du contexte (unicit?? de la r??f??rence, exercie
	 * comptable non clotur??...)
	 *
	 * @param pEcritureComptable -
	 * @throws FunctionalException Si l'Ecriture comptable ne respecte pas les
	 *                             r??gles de gestion
	 */
	// TODO tests ?? compl??ter
	protected void checkEcritureComptableUnit(EcritureComptable pEcritureComptable) throws FunctionalException {

		checkRGCompta0(pEcritureComptable);
		// ===== RG_Compta_2 : Pour qu'une ??criture comptable soit valide, elle doit
		// ??tre ??quilibr??e car il s'agit dun journal comptable (travail du comptable)
		checkRGCompta2(pEcritureComptable);
		
		// ===== RG_Compta_3 : une ??criture comptable doit avoir au moins 2 lignes
		// d'??criture (1 au d??bit, 1 au cr??dit)
		checkRGCompta3(pEcritureComptable);
		
		// FIXME ===== RG_Compta_5 : Format et contenu de la r??f??rence // DONE
		// v??rifier que l'ann??e dans la r??f??rence correspond bien ?? la date de
		// l'??criture, idem pour le code journal...
		checkRGCompta5(pEcritureComptable);
		
		// FIXME delete this comment all as been done
		// ===== V??rification des contraintes unitaires sur les attributs de l'??criture
		/*
		 * Set<ConstraintViolation<EcritureComptable>> vViolations =
		 * getConstraintValidator().validate(pEcritureComptable); if
		 * (!vViolations.isEmpty()) { throw new
		 * FunctionalException("L'??criture comptable ne respecte pas les r??gles de gestion."
		 * , new ConstraintViolationException(
		 * "L'??criture comptable ne respecte pas les contraintes de validation",
		 * vViolations)); }
		 */

		// ===== RG_Compta_2 : Pour qu'une ??criture comptable soit valide, elle doit
		// ??tre ??quilibr??e
		/*
		 * if (!pEcritureComptable.isEquilibree()) { throw new
		 * FunctionalException("L'??criture comptable n'est pas ??quilibr??e."); }
		 */

		// ===== RG_Compta_3 : une ??criture comptable doit avoir au moins 2 lignes
		// d'??criture (1 au d??bit, 1 au cr??dit)
		/*
		 * int vNbrCredit = 0; int vNbrDebit = 0; for (LigneEcritureComptable
		 * vLigneEcritureComptable : pEcritureComptable.getListLigneEcriture()) { if
		 * (BigDecimal.ZERO
		 * .compareTo(ObjectUtils.defaultIfNull(vLigneEcritureComptable.getCredit(),
		 * BigDecimal.ZERO)) != 0) { vNbrCredit++; } if (BigDecimal.ZERO
		 * .compareTo(ObjectUtils.defaultIfNull(vLigneEcritureComptable.getDebit(),
		 * BigDecimal.ZERO)) != 0) { vNbrDebit++; } }
		 *
		 *
		 * // On test le nombre de lignes car si l'??criture ?? une seule ligne // avec un
		 * montant au d??bit et un montant au cr??dit ce n'est pas valable if
		 * (pEcritureComptable.getListLigneEcriture().size() < 2 || vNbrCredit < 1 ||
		 * vNbrDebit < 1) { throw new FunctionalException(
		 * "L'??criture comptable doit avoir au moins deux lignes : une ligne au d??bit et une ligne au cr??dit."
		 * ); }
		 */

		// FIXME ===== RG_Compta_5 : Format et contenu de la r??f??rence // DONE
		// v??rifier que l'ann??e dans la r??f??rence correspond bien ?? la date de
		// l'??criture, idem pour le code journal...
	}

	/**
	 * V??rifie que l'Ecriture comptable respecte les r??gles de gestion li??es au
	 * contexte (unicit?? de la r??f??rence, ann??e comptable non clotur??...)
	 *
	 * @param pEcritureComptable -
	 * @throws FunctionalException Si l'Ecriture comptable ne respecte pas les
	 *                             r??gles de gestion
	 */
	protected void checkEcritureComptableContext(EcritureComptable pEcritureComptable) throws FunctionalException {
		// ===== RG_Compta_6 : La r??f??rence d'une ??criture comptable doit ??tre unique
		if (StringUtils.isNoneEmpty(pEcritureComptable.getReference())) {
			try {
				// Recherche d'une ??criture ayant la m??me r??f??rence
				EcritureComptable vECRef = getDaoProxy().getComptabiliteDao()
						.getEcritureComptableByRef(pEcritureComptable.getReference());

				// Si l'??criture ?? v??rifier est une nouvelle ??criture (id == -1),
				// ou si elle ne correspond pas ?? l'??criture trouv??e (id != idECRef),
				// c'est qu'il y a d??j?? une autre ??criture avec la m??me r??f??rence
				// FIXME modified pEcritureComptable.getId() == null to
				// pEcritureComptable.getId() == -1 as per modification on EcritureComptable
				// bean where default id is not null and value is -1 // DONE;
				if (pEcritureComptable.getId() == -1 || !pEcritureComptable.getId().equals(vECRef.getId())) {
					throw new FunctionalException("Une autre ??criture comptable existe d??j?? avec la m??me r??f??rence.");
				}
			} catch (NotFoundException vEx) {
				// Dans ce cas, c'est bon, ??a veut dire qu'on n'a aucune autre ??criture avec la
				// m??me r??f??rence.
			}
		}
		// as the reference can be null no action on empty reference
	}



	@Override
	public void insertEcritureComptable(EcritureComptable pEcritureComptable)
			throws FunctionalException, NotFoundException {
  
		this.addReference(pEcritureComptable);
		this.checkEcritureComptable(pEcritureComptable);
		TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
		try {
			getDaoProxy().getComptabiliteDao().insertEcritureComptable(pEcritureComptable);
			getTransactionManager().commitMyERP(vTS);
			vTS = null;
		} finally {
			getTransactionManager().rollbackMyERP(vTS);
		}
	}

	// FIXME no check on EcritureComptable // corrected
	/**
	 * {@inheritDoc}
	 *
	 * @throws NotFoundException
	 * @throws FunctionalException
	 */
	@Override
	public void updateEcritureComptable(EcritureComptable pEcritureComptable)
			throws FunctionalException, NotFoundException {
		// after bean correction libelle can't be null, add this loop to avoid any issue
		// on previous correction data in db.
		for (LigneEcritureComptable vLigneEcritureComptable : pEcritureComptable.getListLigneEcriture()) {
			if (vLigneEcritureComptable.getLibelle() == null) {
				vLigneEcritureComptable.setLibelle("[ERROR] Please add a libelle it can't be empty");
			}
		}
		this.checkEcritureComptableUnit(pEcritureComptable);
		this.checkEcritureComptableBeforeUpdate(pEcritureComptable);
		TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
		try {
			getDaoProxy().getComptabiliteDao().updateEcritureComptable(pEcritureComptable);
			getTransactionManager().commitMyERP(vTS);
			vTS = null;
		} finally {
			getTransactionManager().rollbackMyERP(vTS);
		}
	}

//FIXME no check on EcritureComptable existence
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteEcritureComptable(Integer pId) {
		TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
		try {
			getDaoProxy().getComptabiliteDao().deleteEcritureComptable(pId);
			getTransactionManager().commitMyERP(vTS);
			vTS = null;
		} finally {
			getTransactionManager().rollbackMyERP(vTS);
		}
	}

	// ======== Refactoring =============
	// TODO add to report all the following
	/**
	 *
	 * @param pEcritureComptable the EcritreComptable to update
	 * @throws FunctionalException RG_Compta failed
	 * @throws NotFoundException   Not in DB error
	 */
	protected void checkEcritureComptableBeforeUpdate(EcritureComptable pEcritureComptable)
			throws FunctionalException, NotFoundException {
		//TODO check
		this.checkEcritureComptableUnit(pEcritureComptable);
		this.checkIsJournalAndCompteComptableExist(pEcritureComptable);

	}

	// TODO add to report
	// RG_Compta_0
	/**
	 * RG_Compta_0 bean constrain check
	 *
	 * @param pEcritureComptable the EcritureComptable to check
	 * @throws FunctionalException if constrain are not respected RG_Compta 0
	 */
	protected void checkRGCompta0(EcritureComptable pEcritureComptable) throws FunctionalException {
		Set<ConstraintViolation<EcritureComptable>> vViolations = getConstraintValidator().validate(pEcritureComptable);
		if (!vViolations.isEmpty()) {
			throw new FunctionalException("L'??criture comptable ne respecte pas les r??gles de gestion.",
					new ConstraintViolationException(
							"L'??criture comptable ne respecte pas les contraintes de validation", vViolations));
		}

	}
	// TODO add to report

	/**
	 * RG_Compta_2 : Pour qu'une ??criture comptable soit valide, elle doit ??tre
	 * ??quilibr??e
	 *
	 * @param pEcritureComptable the EcritureComptable containing
	 *                           LigneEcritureComptable
	 * @throws FunctionalException if isEquilibree = False
	 */
	protected void checkRGCompta2(EcritureComptable pEcritureComptable) throws FunctionalException {

		if (!pEcritureComptable.isEquilibree()) {
			throw new FunctionalException("L'??criture comptable n'est pas ??quilibr??e.");
		}
	}
	// TODO add to report

	/**
	 * RG_Compta_3<br>
	 * 1- Une ??criture comptable doit contenir au moins deux lignes d'??criture : une
	 * au d??bit et une au cr??dit.<br>
	 * 2- une ??criture comptable doit avoir au moins 2 lignes d'??criture (1 au
	 * d??bit, 1 au cr??dit)
	 *
	 * @param pEcritureComptable EcritureComptable to check
	 * @throws FunctionalException if pEcritureComptable.listLigneEcriture.size is
	 *                             &gt; 2
	 */
	protected void checkRGCompta3(EcritureComptable pEcritureComptable) throws FunctionalException {
		if (!isListLigneEcritureSizePass(pEcritureComptable) && !atLeastOneDebitAndOneCredit(pEcritureComptable)) {
			throw new FunctionalException(
					"L'??criture comptable doit avoir au moins deux lignes : une ligne au d??bit et une ligne au cr??dit.");
		}
	}

	/**
	 * RG_Compta_3 : une ??criture comptable doit avoir au moins 2 lignes d'??criture
	 * (1 au d??bit, 1 au cr??dit)
	 *
	 * @param pEcritureComptable EcritureComptable to check
	 * @return true if pEcritureComptable.listLigneEcriture.size is &gt; 2
	 */
	protected Boolean isListLigneEcritureSizePass(EcritureComptable pEcritureComptable) {
		Integer vSize = pEcritureComptable.getListLigneEcriture().size();
		return vSize >= 2;
	}

	// ===== RG_Compta_3 : une ??criture comptable doit avoir au moins 2 lignes
	// d'??criture (1 au d??bit, 1 au cr??dit)
	/**
	 * RG_Compta_3 : une ??criture comptable doit avoir au moins 2 lignes d'??criture
	 * (1 au d??bit, 1 au cr??dit)
	 *
	 * @param pEcritureComptable The EcritureComptable to test
	 * @return True if a least there is 1 debit value AND 1 credit value else False
	 */
	protected Boolean atLeastOneDebitAndOneCredit(EcritureComptable pEcritureComptable) {
		int vNbrCredit = 0;
		int vNbrDebit = 0;
		// TODO added condition on not 0 sized list
		if (pEcritureComptable.getListLigneEcriture().size() != 0) {

			for (LigneEcritureComptable vLigneEcritureComptable : pEcritureComptable.getListLigneEcriture()) {
				if (BigDecimal.ZERO.compareTo(
						ObjectUtils.defaultIfNull(vLigneEcritureComptable.getCredit(), BigDecimal.ZERO)) != 0) {
					vNbrCredit++;
				}
				if (BigDecimal.ZERO.compareTo(
						ObjectUtils.defaultIfNull(vLigneEcritureComptable.getDebit(), BigDecimal.ZERO)) != 0) {
					vNbrDebit++;
				}
			}
		}
		return vNbrCredit >= 1 && vNbrDebit >= 1;
	}

	protected void checkRGCompta5(EcritureComptable pEcritureComptable) throws FunctionalException {
		String[] vToCompare = splittedEcritureComptableReference(pEcritureComptable);
		if (!checkReferenceDate(pEcritureComptable, vToCompare[1])
				|| !checkReferenceJournalCode(pEcritureComptable, vToCompare[0])) {
			throw new FunctionalException(
					"La R??f??rence de l'??criture comptable contien une erreur sur le code journal et/ou l'ann??e.");
		}
	}

	protected Boolean checkReferenceDate(EcritureComptable pEcritureComptable, String pReferenceYear)
			throws FunctionalException {
		return String.valueOf(getYear(pEcritureComptable)).equals(pReferenceYear);
	}

	protected Integer getYear(EcritureComptable pEcritureComptable) throws FunctionalException {
		return Integer.parseInt(parseDateToString(pEcritureComptable.getDate()).split("[-]+")[0]);
	}

	protected String parseDateToString(Date pDate) throws FunctionalException {
		if (pDate != null) {
			DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd");
			String strDate = dateFormat.format(pDate);
			return strDate;
		} else {
			throw new FunctionalException(" La date est absente");
		}

	}

	protected Boolean checkReferenceJournalCode(EcritureComptable pEcritureComptable, String pReferenceJournalCode) {
		return pReferenceJournalCode.equals(pEcritureComptable.getJournal().getCode());
	}

	/**
	 *
	 * @param pEcritureComptable the EcritureComptable
	 * @return EcritureComptable reference as String array with at index:<br>
	 *         0: journal code<br>
	 *         1: year<br>
	 *         2: reference number
	 */
	protected String[] splittedEcritureComptableReference(EcritureComptable pEcritureComptable) {
		String[] journalCodeSplit = pEcritureComptable.getReference().split("[-/]+");
		return journalCodeSplit;
	}

	// TODO add chek on EcritureComptable id != -1 before insert
	protected void checkEcritureComptableIdNotDefault(EcritureComptable pEcritureComptable) throws TechnicalException {
		if (pEcritureComptable.getId() == -1) {
			throw new TechnicalException("Valeur par defaut appliqu??e, erreur s??quence");
		}
	}

	public void checkIsJournalAndCompteComptableExist(EcritureComptable pEcritureComptable) throws NotFoundException {
		List<JournalComptable> vJournalComptables = new ArrayList<>();
		List<CompteComptable> vCompteComptables = new ArrayList<>();
		vJournalComptables = this.getListJournalComptable();
		vCompteComptables = this.getListCompteComptable();
		checkJournalComptable(pEcritureComptable, vJournalComptables);
		checkCompteComptable(pEcritureComptable, vCompteComptables);

	}

	// TODO add check on code journal not null and check if found in db if not
	// notfound exception
	/**
	 * {@link #isJournalComptableExist(EcritureComptable, List)}
	 *
	 * @param pEcritureComptable    EcritureComptable
	 * @param pJournalComptableList is a list ofJournalComptable
	 * @throws NotFoundException throws error message containing not found code
	 *                           journal.
	 */
	protected void checkJournalComptable(EcritureComptable pEcritureComptable,
			List<JournalComptable> pJournalComptableList) throws NotFoundException {
		String vJournalComptableCode = isJournalComptableExist(pEcritureComptable, pJournalComptableList);
		if (vJournalComptableCode != null) {
			throw new NotFoundException(" Le journal comptable code : [" + vJournalComptableCode + "] n'existe pas");
		}

	}

	/**
	 *
	 * @param pEcritureComptable    EcritureComptable
	 * @param pJournalComptableList List &lt;JournalComptable&gt;
	 * @return null if JournalComptable code exist in the list, the String value of
	 *         the code if not.
	 */
	protected String isJournalComptableExist(EcritureComptable pEcritureComptable,
			List<JournalComptable> pJournalComptableList) {
		String vError = pEcritureComptable.getJournal().getCode();
		if (JournalComptable.getByCode(pJournalComptableList, pEcritureComptable.getJournal().getCode()) == null) {
			return vError;
		}
		return null;
	}

	// TODO add check on code CompteComptable not null and check if found in db if
	// not notfound exception
	/**
	 * {@link #isCompteComptableExist(EcritureComptable, List)}
	 *
	 * @param pEcritureComptable EcritureComptable.class
	 * @param pCompteComptables  a list of CompteComptable;
	 * @throws NotFoundException throws error message containing not found numero
	 *                           CompteComptable.
	 */
	protected void checkCompteComptable(EcritureComptable pEcritureComptable, List<CompteComptable> pCompteComptables)
			throws NotFoundException {
		Integer vCompteComptableInError = isCompteComptableExist(pEcritureComptable, pCompteComptables);
		if (vCompteComptableInError != null) {
			throw new NotFoundException(" Le compte comptable numero : [" + vCompteComptableInError + "] n'existe pas");
		}
	}

	/**
	 *
	 * @param pEcritureComptable EcritureComptable.class
	 * @param pCompteComptables  List of CompteComptable
	 * @return null if CompteComptable numero exist in the list, the Integer value
	 *         of the numero if not.
	 */
	protected Integer isCompteComptableExist(EcritureComptable pEcritureComptable,
			List<CompteComptable> pCompteComptables) {
		Integer vError = null;
		for (LigneEcritureComptable vLigneEcritureComptable : pEcritureComptable.getListLigneEcriture()) {
			Integer vCompteComptableNumero = vLigneEcritureComptable.getCompteComptable().getNumero();
			if (CompteComptable.getByNumero(pCompteComptables, vCompteComptableNumero) == null) {
				return vError = vCompteComptableNumero;
			}
		}
		return vError;
	}

	/**
	 *
	 * @param pEcritureComptable the EcritureComptable to delete
	 * @throws NotFoundException id EcritureComptable is not found
	 */
	public void checkIsEcritureComptableExist(EcritureComptable pEcritureComptable) throws NotFoundException {
		List<EcritureComptable> vEcritureComptables = this.getListEcritureComptable();
		String vReference = isEcritureComptableExist(pEcritureComptable, vEcritureComptables);
		if (vReference != null) {
			throw new NotFoundException(" l'??criture comptable reference : [" + vReference + "] n'existe pas");
		}
	}

	/**
	 *
	 * @param pEcritureComptable  the EcritureComptable to delete
	 * @param pEcritureComptables the List of EcritureComptable wher to seek the one
	 *                            to delete
	 * @return the EcritureComptabel reference if not find else null
	 */
	protected String isEcritureComptableExist(EcritureComptable pEcritureComptable,
			List<EcritureComptable> pEcritureComptables) {
		Integer vSeekedId = pEcritureComptable.getId();
		for (EcritureComptable vEcritureComptable : pEcritureComptables) {
			if (vSeekedId == vEcritureComptable.getId()) {
				return null;
			}
		}
		return pEcritureComptable.getReference();
	}

	@Override
	public SequenceEcritureComptable getSequenceEcritureComptable(String pJournalCode, Integer pAnnee)
			throws NotFoundException {
		return getDaoProxy().getComptabiliteDao().getSequenceEcritureComptable(pJournalCode, pAnnee);
	}

	@Override
	public void insertSequenceEcritureComptable(SequenceEcritureComptable pSequenceEcritureComptable) {
		TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
		try {
			getDaoProxy().getComptabiliteDao().insertSequenceEcritureComptable(pSequenceEcritureComptable);
			getTransactionManager().commitMyERP(vTS);
			vTS = null;
		} finally {
			getTransactionManager().rollbackMyERP(vTS);
		}

	}

	@Override
	public void updateSequenceEcritureComptable(SequenceEcritureComptable pSequenceEcritureComptable) {
		TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
		try {
			getDaoProxy().getComptabiliteDao().updateSequenceEcritureComptable(pSequenceEcritureComptable);
			getTransactionManager().commitMyERP(vTS);
			vTS = null;
		} finally {
			getTransactionManager().rollbackMyERP(vTS);
		}

	}

}
