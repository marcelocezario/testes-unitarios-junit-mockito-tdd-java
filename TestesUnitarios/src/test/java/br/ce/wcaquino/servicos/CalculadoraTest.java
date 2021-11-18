package br.ce.wcaquino.servicos;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import br.ce.wcaquino.exceptions.NaoPodeDividirPorZeroException;

public class CalculadoraTest {

	public static StringBuffer ordem = new StringBuffer();

	private Calculadora calc;

	@Before
	public void setup() {
		calc = new Calculadora();
		System.out.println("iniciando...");
		ordem.append("1");
	}

	@After
	public void tearDown() {
		System.out.println("finalizando...");
	}

	@AfterClass
	public static void tearDownClass() {
		System.out.println(ordem.toString());
	}

	@Test
	public void deveSomarDoisValores() {
		// cenário
		int a = 5;
		int b = 3;

		// ação
		int resultado = calc.somar(a, b);

		// verificação
		Assert.assertEquals(8, resultado);
	}

	@Test
	public void deveSubtrairDoisValores() {
		// cenario
		int a = 8;
		int b = 5;

		// ação
		int resultado = calc.subtrair(a, b);

		// verificação
		Assert.assertEquals(3, resultado);
	}

	@Test
	public void deveDividirDoisValores() throws NaoPodeDividirPorZeroException {
		// cenario
		int a = 6;
		int b = 3;

		// ação
		int resultado = calc.dividir(a, b);

		// verificação
		Assert.assertEquals(2, resultado);
	}

	@Test(expected = NaoPodeDividirPorZeroException.class)
	public void deveLancarExcecaoAoDividirPorZero() throws NaoPodeDividirPorZeroException {
		int a = 10;
		int b = 0;

		calc.dividir(a, b);
	}

	@Test
	public void deveMultiplicarDoisValores() {
		int a = 6;
		int b = 3;
		Calculadora calc = new Calculadora();

		int resultado = calc.multiplicar(a, b);

		// verificação
		Assert.assertEquals(18, resultado);
	}

}
