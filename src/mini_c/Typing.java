package mini_c;

public class Typing implements Pvisitor {

	// le résultat du typage sera mis dans cette variable
	private File file;
	// et renvoyé par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}
	
	// il faut compléter le visiteur ci-dessous pour réaliser le typage
	
	@Override
	public void visit(Pfile n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PTint n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(PTstruct n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pint n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pident n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Punop n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Passign n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pbinop n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Parrow n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pcall n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Psizeof n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pskip n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Peval n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pif n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pwhile n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pbloc n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Preturn n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pstruct n) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(Pfun n) {
		// TODO Auto-generated method stub
		
	}

}
